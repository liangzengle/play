package play.db.r2dbc

import com.fasterxml.jackson.core.JsonProcessingException
import io.r2dbc.spi.Connection
import io.r2dbc.spi.ConnectionFactory
import io.r2dbc.spi.Result
import java.sql.DriverManager
import java.util.*
import javax.inject.Inject
import play.ApplicationLifecycle
import play.ClassScanner
import play.db.*
import play.getLogger
import play.util.concurrent.Future
import play.util.concurrent.Promise
import play.util.json.Json.toJsonString
import play.util.reflect.isAssignableFrom
import reactor.core.publisher.Mono

/**
 *
 * @author LiangZengle
 */
class R2DBCRepository @Inject constructor(
  applicationLifecycle: ApplicationLifecycle,
  private val connectionFactory: ConnectionFactory,
  private val tableNameResolver: TableNameResolver,
  private val conf: DBConfig,
  private val classScanner: ClassScanner
) : Repository(applicationLifecycle) {

  private val logger = getLogger()

  init {
    createConnectionNoDB().use { conn ->
      val createDB = createDatabaseIfNotExists(conn, dbName())
      if (createDB) {
        logger.info { "创建数据库: ${dbName()}" }
      }
      val nameToClass =
        classScanner.getOrdinarySubTypesSequence(Entity::class.java).map { tableNameOf(it) to it }.toMap()
      val createdTables = createTablesIfNotExists(conn, nameToClass)
      if (createdTables.isNotEmpty()) {
        logger.info { "创建表: $createdTables" }
      }
    }
  }

  companion object {
    const val ID = "`id`"
    const val Data = "`data`"
  }

  private fun dbName() = conf.dbName

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

  private fun createConnectionNoDB(): java.sql.Connection {
    return DriverManager.getConnection(conf.getUrlNoDB(), conf.username, conf.password)
  }

  private fun createTablesIfNotExists(
    connection: java.sql.Connection,
    tables: Map<String, Class<out Entity<*>>>
  ): Collection<String> {
    return connection.use { conn ->
      val tablesToCreate = conn.createStatement().use { stmt ->
        stmt.executeQuery("SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = '${dbName()}'")
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
                CREATE TABLE IF NOT EXISTS `${dbName()}`.`${tableNameOf(clazz)}` (
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

  private fun getConn(): Mono<Connection> {
    return Mono.from(connectionFactory.create())
  }

  private fun createDatabaseIfNotExists(conn: java.sql.Connection, dbName: String): Boolean {
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

  override fun insert(entity: Entity<*>): Future<out Any> {
    val promise = Promise.make<Result>()
    getConn().subscribe { conn ->
      conn.createStatement("INSERT INTO ${tableNameOf(entity.javaClass)}($ID, $Data) VALUES(?,?)")
        .bind(1, entity.id())
        .bind(2, jsonify(entity))
        .execute()
        .subscribe(ForOneSubscriber(promise))
    }
    return promise.future
  }

  override fun update(entity: Entity<*>): Future<out Any> {
    TODO("Not yet implemented")
  }

  override fun insertOrUpdate(entity: Entity<*>): Future<out Any> {
    TODO("Not yet implemented")
  }

  override fun delete(entity: Entity<*>): Future<out Any> {
    TODO("Not yet implemented")
  }

  override fun <ID, E : Entity<ID>> deleteById(id: ID, entityClass: Class<E>): Future<out Any> {
    TODO("Not yet impl emented")
  }

  override fun batchInsertOrUpdate(entities: Collection<Entity<*>>): Future<out Any> {
    TODO("Not yet implemented")
  }

  override fun <ID, E : Entity<ID>> findById(id: ID, entityClass: Class<E>): Future<Optional<E>> {
    TODO("Not yet implemented")
  }

  override fun <ID, E : Entity<ID>> listAll(entityClass: Class<E>): Future<List<E>> {
    TODO("Not yet implemented")
  }

  override fun <ID, E : Entity<ID>> listIds(entityClass: Class<E>): Future<List<ID>> {
    TODO("Not yet implemented")
  }

  override fun <ID, E : Entity<ID>> query(
    entityClass: Class<E>,
    where: Optional<String>,
    order: Optional<String>,
    limit: Optional<Int>
  ): Future<List<E>> {
    TODO("Not yet implemented")
  }

  override fun <ID, E : Entity<ID>> query(
    entityClass: Class<E>,
    fields: List<String>,
    where: Optional<String>,
    order: Optional<String>,
    limit: Optional<Int>
  ): Future<List<ResultMap>> {
    TODO("Not yet implemented")
  }

  override fun <ID, E : Entity<ID>, R, R1 : R> fold(
    entityClass: Class<E>,
    where: Optional<String>,
    order: Optional<String>,
    limit: Optional<Int>,
    initial: R1,
    folder: (R1, E) -> R1
  ): Future<R> {
    TODO("Not yet implemented")
  }

  override fun <ID, E : Entity<ID>, R, R1 : R> fold(
    entityClass: Class<E>,
    fields: List<String>,
    where: Optional<String>,
    order: Optional<String>,
    limit: Optional<Int>,
    initial: R1,
    folder: (R1, ResultMap) -> R1
  ): Future<R> {
    TODO("Not yet implemented")
  }

  override fun close() {
    TODO("Not yet implemented")
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
    checkNotNull(ex)
    logger.error(ex) { "json序列化失败: $entity" }
    throw ex
  }
}
