package play.db.jdbc.mysql

import io.vavr.concurrent.Future
import io.vavr.concurrent.Promise
import io.vavr.control.Option
import io.vavr.kotlin.none
import io.vavr.kotlin.option
import play.ClassScanner
import play.db.*
import play.db.jdbc.JdbcConfiguration
import play.getLogger
import play.util.json.Json
import play.util.json.Json.toJsonString
import java.io.Closeable
import java.sql.ResultSet
import java.util.*
import java.util.concurrent.Callable
import javax.annotation.CheckReturnValue
import javax.inject.Inject
import javax.inject.Singleton
import javax.sql.DataSource

@Singleton
class MysqlRepository @Inject constructor(
  private val executor: DbExecutor,
  private val ds: DataSource,
  classScanner: ClassScanner,
  private val tableNameResolver: TableNameResolver,
  private val conf: JdbcConfiguration
) : Repository {

  private val logger = getLogger()

  companion object {
    const val ID = "`id`"
    const val Data = "`data`"
  }

  init {
    val createDB = createDatabaseIfNotExists(dbName())
    if (createDB) {
      logger.info { "创建数据库: ${dbName()}" }
    }
    val nameToClass =
      classScanner.getSubTypesSequence(Entity::class.java).map { tableNameOf(it) to it }.toMap()
    val createdTables = createTablesIfNotExists(nameToClass)
    if (createdTables.isNotEmpty()) {
      logger.info { "创建表: $createdTables" }
    }
  }

  private fun dbName() = conf.db

  private fun createTablesIfNotExists(tables: Map<String, Class<out Entity<*>>>): Collection<String> {
    return ds.connection.use { conn ->
      val tablesToCreate =
        conn.prepareStatement("SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = ?")
          .use { stmt ->
            stmt.setString(1, dbName())
            stmt.executeQuery().use { rs ->
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
        val sql = """
                CREATE TABLE IF NOT EXISTS ${qualifiedTableName(clazz)} (
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

  private fun createDatabaseIfNotExists(dbName: String): Boolean {
    return ds.connection.use { conn ->
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
      !dbExists
    }
  }

  private fun qualifiedTableName(clazz: Class<*>): String {
    return "`${dbName()}`.`${tableNameOf(clazz)}`"
  }

  private fun tableNameOf(clazz: Class<*>): String = tableNameResolver.resolve(clazz)

  private fun idJdbcType(clazz: Class<out Entity<*>>): String {
    return when (clazz.superclass) {
      EntityInt::class.java -> "int"
      EntityLong::class.java -> "bigint"
      EntityString::class.java -> "varchar(255)"
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
        val sql = "INSERT INTO ${qualifiedTableName(entity.javaClass)}($ID, $Data) VALUES(?,?)"
        conn.prepareStatement(sql).use { stmt ->
          stmt.setObject(1, entity.id())
          stmt.setObject(2, entity.toJsonString())
          stmt.executeUpdate()
        }
      }
    }
  }

  override fun update(entity: Entity<*>): Future<out Any> {
    return exec {
      ds.connection.use { conn ->
        val sql = "UPDATE ${qualifiedTableName(entity.javaClass)} SET $Data=? WHERE $ID=?"
        conn.prepareStatement(sql).use { stmt ->
          stmt.setObject(1, entity.toJsonString())
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
          "INSERT INTO ${qualifiedTableName(entity.javaClass)}($ID, $Data) VALUES(?,?) ON DUPLICATE KEY UPDATE $Data = VALUES($Data)"
        conn.prepareStatement(sql).use { stmt ->
          stmt.setObject(1, entity.id())
          stmt.setObject(2, entity.toJsonString())
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
    return exec({
      ds.connection.use { conn ->
        val sql = "DELETE FROM ${qualifiedTableName(entityClass)} WHERE $ID = ?"
        conn.prepareStatement(sql).use { stmt ->
          stmt.setObject(1, id)
          stmt.executeUpdate()
        }
      }
    })
  }

  override fun batchInsertOrUpdate(entities: Collection<Entity<*>>): Future<Int> {
    if (entities.isEmpty()) return Future.successful(0)
    return exec {
      ds.connection.use { conn ->
        val sql =
          "INSERT INTO ${qualifiedTableName(entities.first().javaClass)}($ID, $Data) VALUES(?,?) ON DUPLICATE KEY UPDATE $Data = VALUES($Data)"
        conn.prepareStatement(sql).use { stmt ->
          entities.forEach {
            stmt.setObject(1, it.id())
            stmt.setObject(2, it.toJsonString())
            stmt.addBatch()
          }
          stmt.executeUpdate()
        }
      }
    }
  }

  override fun <ID, E : Entity<ID>> findById(id: ID, entityClass: Class<E>): Future<Option<E>> {
    return exec {
      ds.connection.use { conn ->
        val sql = "SELECT $Data FROM ${qualifiedTableName(entityClass)} WHERE $ID = ?"
        conn.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY).use { stmt ->
          stmt.setObject(1, id)
          val rs = stmt.executeQuery()
          val a: Option<E> = if (rs.next()) {
            val data = rs.getString(1)
            Json.toObject(data, entityClass).option()
          } else {
            none()
          }
          a
        }
      }
    }
  }

  override fun <ID, E : Entity<ID>> listAll(entityClass: Class<E>): Future<List<E>> {
    return exec {
      ds.connection.use { conn ->
        val sql = "SELECT $Data FROM ${qualifiedTableName(entityClass)}"
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
      val sql = "SELECT $Data FROM ${qualifiedTableName(entityClass)}"
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
    return promise.future()
  }

  override fun <ID, E : Entity<ID>> listIds(entityClass: Class<E>): Future<List<ID>> {
    return exec {
      ds.connection.use { conn ->
        val sql = "SELECT $ID FROM ${qualifiedTableName(entityClass)}"
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
    where: Option<String>,
    order: Option<String>,
    limit: Option<Int>
  ): Future<List<E>> {
    return exec {
      ds.connection.use { conn ->
        conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY).use { stmt ->
          val b = StringBuilder()
          b.append("SELECT ").append(Data).append(" FROM ").append(qualifiedTableName(entityClass))
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
    where: Option<String>,
    order: Option<String>,
    limit: Option<Int>
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
    where: Option<String>,
    order: Option<String>,
    limit: Option<Int>,
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
    return promise.future()
  }

  private fun buildSql(
    entityClass: Class<*>,
    fields: List<String>,
    where: Option<String>,
    order: Option<String>,
    limit: Option<Int>
  ): String {
    return """
      SELECT JSON_OBJECT('id', id, ${fields.asSequence().map { "'$it', t.$it" }.joinToString()})
      FROM
      (
      SELECT ${fields.asSequence().map { "$ID, ${dataProp(it)} as $it" }.joinToString()}
      FROM ${qualifiedTableName(entityClass)}
      ${where.fold({ "" }) { " WHERE $it" }}
      ${order.fold({ "" }) { " ORDER BY $it" }}
      ${limit.fold({ "" }) { " LIMIT $it" }}
      ) t
      """.trimIndent()
  }

  private fun <T : Any> exec(task: Callable<T>): Future<T> {
    val f = executor.submit(task)
    return Future.fromJavaFuture(executor, f)
  }
}
