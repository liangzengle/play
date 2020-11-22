package play.db.jdbc.mysql

import com.fasterxml.jackson.core.JsonProcessingException
import play.*
import play.db.*
import play.db.jdbc.JdbcConfiguration
import play.util.concurrent.Future
import play.util.concurrent.Promise
import play.util.fold
import play.util.forEach
import play.util.json.Json
import play.util.json.Json.toJsonString
import play.util.reflect.isAssignableFrom
import play.util.toOptional
import java.io.Closeable
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.util.*
import java.util.concurrent.Callable
import javax.annotation.CheckReturnValue
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton
import javax.sql.DataSource

@Singleton
class MysqlRepository @Inject constructor(
  private val executor: DbExecutor,
  datasourceProvider: Provider<DataSource>,
  classScanner: ClassScanner,
  private val tableNameResolver: TableNameResolver,
  private val conf: JdbcConfiguration,
  @Named("mysql") configuration: Configuration,
  lifecycle: ApplicationLifecycle
) : Repository(lifecycle) {

  private val logger = getLogger()

  private val batchSize = configuration.getInt("batch-size")

  private val ds: DataSource

  companion object {
    const val ID = "`id`"
    const val Data = "`data`"
  }

  init {
    createConnectionNoDB().use { conn ->
      checkRequirements(conn, configuration)
      val createDB = createDatabaseIfNotExists(conn, dbName())
      if (createDB) {
        logger.info { "创建数据库: ${dbName()}" }
      }
      val nameToClass =
        classScanner.getConcreteSubTypesSequence(Entity::class.java).map { tableNameOf(it) to it }.toMap()
      val createdTables = createTablesIfNotExists(conn, nameToClass)
      if (createdTables.isNotEmpty()) {
        logger.info { "创建表: $createdTables" }
      }
    }
    ds = datasourceProvider.get()
  }

  private fun createConnectionNoDB(): Connection {
    return DriverManager.getConnection(conf.getUrlNoDB(), conf.username, conf.password)
  }

  private fun checkRequirements(conn: Connection, conf: Configuration) {
    val mysqlMajorVersion = conf.getInt("required-major-version")
    val requiredPacketSize = conf.getMemorySize("required-packet-size").toBytes()
    val panicOnSmallPacketSize = conf.getBoolean("panic-on-small-packet-size")
    if (conn.metaData.driverMajorVersion < mysqlMajorVersion) {
      throw IllegalStateException("Require mysql driver version $mysqlMajorVersion or higher")
    }
    if (conn.metaData.databaseMajorVersion < mysqlMajorVersion) {
      throw IllegalStateException("Require mysql server version $mysqlMajorVersion or higher")
    }
    val stmt = conn.createStatement()
    val rs = stmt.executeQuery("SHOW GLOBAL VARIABLES LIKE 'max_allowed_packet'")
    if (rs.next()) {
      val value = rs.getLong(2)
      if (value < requiredPacketSize) {
        if (panicOnSmallPacketSize) {
          throw IllegalStateException("MySQL variable `max_allowed_packet` is too small: $value")
        }
        Log.warn { "MySQL variable `max_allowed_packet` is too small: $value" }
        val success = stmt.execute("SET GLOBAL max_allowed_packet = $requiredPacketSize")
        if (success) {
          Log.warn { "Changing `max_allowed_packet` to $requiredPacketSize" }
        }
      }
    }
  }

  private fun dbName() = conf.db

  private fun createTablesIfNotExists(
    connection: Connection,
    tables: Map<String, Class<out Entity<*>>>
  ): Collection<String> {
    return connection.use { conn ->
      val tablesToCreate = conn.createStatement().use { stmt ->
        stmt.executeQuery("SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = ${dbName()}")
          .use { rs ->
            val tableExisted = mutableSetOf<String>()
            while (rs.next()) {
              val name = rs.getString(1)
              tableExisted.add(name)
            }
            tables.keys - tableExisted
          }
      }
      tablesToCreate.forEach { tableName ->
        val clazz: Class<out Entity<*>> = tables[tableName] ?: error(tableName)
        val sql =
          """
                CREATE TABLE IF NOT EXISTS ${tableNameOf(clazz)} (
                    $ID ${idJdbcType(clazz)} NOT NULL,
                    $Data json NOT NULL,
                    PRIMARY KEY ($ID)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
          """.trimIndent()
        conn.createStatement().use { stmt ->
          stmt.executeUpdate(sql)
        }
      }
      tablesToCreate
    }
  }

  private fun createDatabaseIfNotExists(conn: Connection, dbName: String): Boolean {
    val dbExists = conn.createStatement().use { stmt ->
      stmt.executeQuery("SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = '$dbName'")
        .use { rs ->
          rs.next()
        }
    }
    if (!dbExists) {
      conn.createStatement().use { stmt ->
        stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS $dbName DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci")
      }
    }
    return !dbExists
  }

  private fun tableNameOf(clazz: Class<*>): String = tableNameResolver.resolve(clazz)

  private fun idJdbcType(clazz: Class<out Entity<*>>): String {
    return when {
      isAssignableFrom<EntityInt>(clazz) -> "int"
      isAssignableFrom<EntityLong>(clazz) -> "bigint"
      isAssignableFrom<EntityString>(clazz) -> "varchar(255)"
      else -> throw UnsupportedOperationException("Unsupported id type: ${clazz.simpleName}")
    }
  }

  private fun dataProp(name: String): String {
    return "$Data->'$.$name'"
  }

  override fun close() {
    if (ds is Closeable) ds.close()
  }

  override fun insert(entity: Entity<*>): Future<out Any> {
    return exec {
      ds.connection.use { conn ->
        val sql = "INSERT INTO ${tableNameOf(entity.javaClass)}($ID, $Data) VALUES(?,?)"
        conn.prepareStatement(sql).use { stmt ->
          stmt.setObject(1, entity.id())
          stmt.setString(2, jsonify(entity))
          stmt.executeUpdate()
        }
      }
    }
  }

  override fun update(entity: Entity<*>): Future<out Any> {
    return exec {
      ds.connection.use { conn ->
        val sql = "UPDATE ${tableNameOf(entity.javaClass)} set $Data = ? WHERE $ID = ?"
        conn.prepareStatement(sql).use { stmt ->
          stmt.setString(1, jsonify(entity))
          stmt.setObject(2, entity.id())
          stmt.executeUpdate()
        }
      }
    }
  }

  override fun insertOrUpdate(entity: Entity<*>): Future<out Any> {
    return exec {
      ds.connection.use { conn ->
        val sql =
          "INSERT INTO ${this.tableNameOf(entity.javaClass)}($ID, $Data) VALUES(?,?) ON DUPLICATE KEY UPDATE $Data = VALUES($Data)"
        conn.prepareStatement(sql).use { stmt ->
          stmt.setObject(1, entity.id())
          stmt.setString(2, jsonify(entity))
          stmt.executeUpdate()
        }
      }
    }
  }

  override fun delete(entity: Entity<*>): Future<out Any> =
    deleteById0(entity.id(), entity.javaClass)

  override fun <ID, E : Entity<ID>> deleteById(id: ID, entityClass: Class<E>): Future<out Any> =
    deleteById0(id as Any, entityClass)

  private fun deleteById0(id: Any, entityClass: Class<*>): Future<out Any> {
    return exec {
      ds.connection.use { conn ->
        val sql = "DELETE FROM ${this.tableNameOf(entityClass)} WHERE $ID = ?"
        conn.prepareStatement(sql).use { stmt ->
          stmt.setObject(1, id)
          stmt.executeUpdate()
        }
      }
    }
  }

  override fun batchInsertOrUpdate(entities: Collection<Entity<*>>): Future<Int> {
    if (entities.isEmpty()) return Future.successful(0)
    return exec {
      ds.connection.use { conn ->
        val sql =
          "INSERT INTO ${this.tableNameOf(entities.first().javaClass)}($ID, $Data) VALUES(?,?) ON DUPLICATE KEY UPDATE $Data = VALUES($Data)"
        val statement = conn.prepareStatement(sql)
        val updatedCount = statement.use { stmt ->
          var count = 0
          var updated = 0
          entities.forEach {
            stmt.setObject(1, it.id())
            stmt.setString(2, jsonify(it))
            stmt.addBatch()
            count += 1
            if (count >= batchSize) {
              updated += stmt.executeBatch().sum()
              count -= batchSize
            }
          }
          if (count > 0) {
            updated += stmt.executeBatch().sum()
          }
          updated
        }
        updatedCount
      }
    }
  }

  override fun <ID, E : Entity<ID>> findById(id: ID, entityClass: Class<E>): Future<Optional<E>> {
    return exec {
      ds.connection.use { conn ->
        val sql = "SELECT $Data FROM ${this.tableNameOf(entityClass)} WHERE $ID = ?"
        conn.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY).use { stmt ->
          stmt.setObject(1, id)
          val rs = stmt.executeQuery()
          val a: Optional<E> = if (rs.next()) {
            val data = rs.getString(1)
            Json.toObject(data, entityClass).toOptional()
          } else {
            Optional.empty()
          }
          a
        }
      }
    }
  }

  override fun <ID, E : Entity<ID>> listAll(entityClass: Class<E>): Future<List<E>> {
    return exec {
      ds.connection.use { conn ->
        val sql = "SELECT $Data FROM ${this.tableNameOf(entityClass)}"
        conn.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY).use { stmt ->
          stmt.fetchSize = Int.MIN_VALUE
          val rs = stmt.executeQuery()
          val result = LinkedList<E>()
          while (rs.next()) {
            val data = rs.getString(1)
            val entity = Json.toObject(data, entityClass)
            result.add(entity)
          }
          result
        }
      }
    }
  }

  @CheckReturnValue
  override fun <ID, E : Entity<ID>, R> fold(
    entityClass: Class<E>,
    initial: R,
    f: (R, E) -> R
  ): Future<R> {
    val promise = Promise.make<R>()
    ds.connection.use { conn ->
      var result = initial
      val sql = "SELECT $Data FROM ${this.tableNameOf(entityClass)}"
      conn.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY).use { stmt ->
        stmt.fetchSize = Int.MIN_VALUE
        val rs = stmt.executeQuery()
        while (rs.next()) {
          val data = rs.getString(1)
          val entity = Json.toObject(data, entityClass)
          result = f(result, entity)
        }
        promise.success(result)
      }
    }
    return promise.future
  }

  override fun <ID, E : Entity<ID>> listIds(entityClass: Class<E>): Future<List<ID>> {
    return exec {
      ds.connection.use { conn ->
        val sql = "SELECT $ID FROM ${this.tableNameOf(entityClass)}"
        conn.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY).use { stmt ->
          stmt.fetchSize = Int.MIN_VALUE
          val rs = stmt.executeQuery()
          val result = LinkedList<ID>()
          while (rs.next()) {
            @Suppress("UNCHECKED_CAST")
            val id = rs.getObject(1) as ID
            result.add(id)
          }
          result
        }
      }
    }
  }

  override fun <ID, E : Entity<ID>> query(
    entityClass: Class<E>,
    where: Optional<String>,
    order: Optional<String>,
    limit: Optional<Int>
  ): Future<List<E>> {
    return exec {
      ds.connection.use { conn ->
        conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY).use { stmt ->
          val b = StringBuilder()
          b.append("SELECT ").append(Data).append(" FROM ").append(this.tableNameOf(entityClass))
          where.forEach { b.append(" WHERE ").append(it) }
          order.forEach { b.append(" ORDER BY ").append(it) }
          limit.forEach { b.append(" LIMIT ").append(it) }
          val sql = b.toString()
          stmt.fetchSize = Int.MIN_VALUE
          val rs = stmt.executeQuery(sql)
          val result = LinkedList<E>()
          while (rs.next()) {
            val data = rs.getString(1)
            val entity = Json.toObject(data, entityClass)
            result.add(entity)
          }
          result
        }
      }
    }
  }

  override fun <ID, E : Entity<ID>> query(
    entityClass: Class<E>,
    fields: List<String>,
    where: Optional<String>,
    order: Optional<String>,
    limit: Optional<Int>
  ): Future<List<ResultMap>> {
    return exec {
      ds.connection.use { conn ->
        conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY).use { stmt ->
          val sql = buildSql(entityClass, fields, where, order, limit)
          stmt.fetchSize = Int.MIN_VALUE
          val rs = stmt.executeQuery(sql)
          val result = LinkedList<ResultMap>()
          while (rs.next()) {
            val data = rs.getString(1)
            val map = Json.to<Map<String, Any?>>(data)
            result.add(ResultMap(map))
          }
          result
        }
      }
    }
  }

  @CheckReturnValue
  override fun <ID, E : Entity<ID>, R> fold(
    entityClass: Class<E>,
    fields: List<String>,
    where: Optional<String>,
    order: Optional<String>,
    limit: Optional<Int>,
    initial: R,
    folder: (R, ResultMap) -> R
  ): Future<R> {
    val promise = Promise.make<R>()
    ds.connection.use { conn ->
      conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY).use { stmt ->
        val sql = buildSql(entityClass, fields, where, order, limit)
        stmt.fetchSize = Int.MIN_VALUE
        var result = initial
        val rs = stmt.executeQuery(sql)
        while (rs.next()) {
          val data = rs.getString(1)
          val map = Json.to<Map<String, Any?>>(data)
          result = folder(result, ResultMap(map))
        }
        promise.success(result)
      }
    }
    return promise.future
  }

  private fun buildSql(
    entityClass: Class<*>,
    fields: List<String>,
    where: Optional<String>,
    order: Optional<String>,
    limit: Optional<Int>
  ): String {
    return """
      SELECT JSON_OBJECT('id', id, ${fields.joinToString { "'$it', t.$it" }})
      FROM
      (
      SELECT ${fields.joinToString { "$ID, ${dataProp(it)} as $it" }}
      FROM ${this.tableNameOf(entityClass)}
      ${where.fold("") { " WHERE $it" }}
      ${order.fold("") { " ORDER BY $it" }}
      ${limit.fold("") { " LIMIT $it" }}
      ) t
    """.trimIndent()
  }

  private fun jsonify(entity: Entity<*>): String {
    var attempts = 5
    var ex: Exception? = null
    while (attempts > 0) {
      attempts--
      try {
        return entity.toJsonString()
      } catch (e: JsonProcessingException) {
        ex = e
        Thread.yield()
      }
    }
    ex!!
    logger.error(ex) { "json序列化失败: $entity" }
    throw ex
  }

  private fun <T : Any> exec(task: Callable<T>): Future<T> {
    val f = executor.submit(task)
    return Future.fromJava(f, executor)
  }
}
