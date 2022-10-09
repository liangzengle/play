package play.db.mongo

import com.mongodb.bulk.BulkWriteResult
import com.mongodb.client.model.*
import com.mongodb.client.result.DeleteResult
import com.mongodb.client.result.InsertOneResult
import com.mongodb.client.result.UpdateResult
import com.mongodb.reactivestreams.client.MongoClient
import com.mongodb.reactivestreams.client.MongoCollection
import org.bson.Document
import org.bson.conversions.Bson
import play.db.Repository
import play.db.ResultMap
import play.db.TableNameResolver
import play.db.mongo.Mongo.ID
import play.entity.Entity
import play.entity.EntityHelper
import play.util.*
import play.util.concurrent.Future
import play.util.concurrent.Future.Companion.toPlay
import play.util.json.Json
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.lang.reflect.Type
import java.util.*

class MongoDBRepository constructor(
  dbName: String,
  private val tableNameResolver: TableNameResolver,
  client: MongoClient
) : Repository, MongoQueryService, MongoDBCommandSupport {

  private val db = client.getDatabase(dbName)

  private val unorderedBulkWrite = BulkWriteOptions().ordered(false)
  private val insertOneOptions = InsertOneOptions()

  //  private val updateOptions = UpdateOptions().upsert(true)
  private val replaceOptions = ReplaceOptions().upsert(true)
  private val deleteOptions = DeleteOptions()

  private fun <T : Entity<*>> getCollection(entity: T): MongoCollection<T> {
    return getCollection(entity.javaClass)
  }

  internal fun <T : Entity<*>> getCollection(clazz: Class<T>): MongoCollection<T> {
    return db.getCollection(getCollectionName(clazz), clazz)
  }

  internal fun getCollectionName(clazz: Class<*>) = tableNameResolver.resolve(clazz)

  fun listCollectionNames(): Flux<String> {
    return Flux.from(db.listCollectionNames())
  }

  internal fun <T : Entity<*>> createCollection(clazz: Class<T>): Mono<Void> {
    return Flux.from(db.createCollection(getCollectionName(clazz))).ignoreElements()
  }

  private fun getRawCollection(clazz: Class<*>): MongoCollection<Document> {
    return db.getCollection(tableNameResolver.resolve(clazz))
  }

  override fun insert(entity: Entity<*>): Future<InsertOneResult> {
    val publisher = getCollection(entity).insertOne(entity, insertOneOptions)
    return Mono.from(publisher).toFuture().toPlay()
  }

  override fun update(entity: Entity<*>): Future<UpdateResult> {
    return insertOrUpdate(entity)
  }

  override fun insertOrUpdate(entity: Entity<*>): Future<UpdateResult> {
    val publisher = getCollection(entity).replaceOne(Filters.eq(entity.id()), entity, replaceOptions)
    return Mono.from(publisher).toFuture().toPlay()
  }

  override fun delete(entity: Entity<*>): Future<DeleteResult> {
    return deleteById(entity.id(), entity.javaClass.unsafeCast())
  }

  override fun <ID, E : Entity<ID>> deleteById(id: ID, entityClass: Class<E>): Future<DeleteResult> {
    val publisher = getCollection(entityClass).deleteOne(Filters.eq(id), deleteOptions)
    return Mono.from(publisher).toFuture().toPlay()
  }

  override fun batchInsertOrUpdate(entities: Collection<Entity<*>>): Future<BulkWriteResult> {
    if (entities.isEmpty()) {
      return Future.successful(BulkWriteResult.acknowledged(0, 0, 0, 0, emptyList(), emptyList()))
    }
    // mongo driver will divide into small groups when a group exceeds the limit, which is 100,000 in MongoDB 3.6
    val writeModels = entities.map { ReplaceOneModel(Filters.eq(it.id()), it, replaceOptions) }
    val publisher = getCollection(entities.first().javaClass).bulkWrite(writeModels, unorderedBulkWrite)
    return Mono.from(publisher).toFuture().toPlay()
  }

  override fun <ID, E : Entity<ID>> update(
    entityClass: Class<E>,
    id: ID,
    field: String,
    value: Any
  ): Future<UpdateResult> {
    val publisher = getCollection(entityClass).updateOne(Filters.eq(ID, id), Updates.set(field, value))
    return Mono.from(publisher).toFuture().toPlay()
  }

  override fun <ID, E : Entity<ID>> findById(entityClass: Class<E>, id: ID): Mono<E> {
    val publisher = getCollection(entityClass).find(Filters.eq(id))
    return Mono.from(publisher)
  }

  override fun <ID, E : Entity<ID>> loadAll(entityClass: Class<E>, ids: Iterable<ID>): Flux<E> {
    val publisher = getCollection(entityClass).find(Filters.`in`(ID, ids))
    return Flux.from(publisher)
  }

  override fun <IDX, ID, E : Entity<ID>> queryIdsByIndex(
    entityClass: Class<E>,
    indexName: String,
    indexValue: IDX
  ): Flux<ID> {
    val idType = EntityHelper.getIdType(entityClass)
    val fieldName = if (indexName.startsWith("id.")) "_$indexName" else indexName
    val where = Filters.and(Filters.eq(fieldName, indexValue), Filters.ne(Entity.DELETED, true))
    return query(entityClass, listOf(ID), where.toOptional(), empty(), emptyInt())
      .map { r -> convertToID<ID>(r.getObject(ID), idType) }
  }

  override fun <IDX, ID, E : Entity<ID>> loadIdsByCacheIndex(entityClass: Class<E>, indexValue: IDX): Flux<ID> {
    return queryIdsByIndex(entityClass, Mongo.getCacheIndexName(entityClass), indexValue)
  }

  override fun <ID, E : Entity<ID>> queryAll(entityClass: Class<E>): Flux<E> {
    val publisher = getCollection(entityClass).find()
    return Flux.from(publisher)
  }

  override fun <ID, E : Entity<ID>> query(
    entityClass: Class<E>,
    where: Optional<Bson>,
    order: Optional<Bson>,
    limit: OptionalInt
  ): Flux<E> {
    val publisher = getCollection(entityClass).find()
    where.forEach { publisher.filter(it) }
    order.forEach { publisher.sort(it) }
    limit.forEach { publisher.limit(it) }
    return Flux.from(publisher)
  }

  override fun <ID, E : Entity<ID>> query(
    entityClass: Class<E>,
    fields: List<String>,
    where: Optional<Bson>,
    order: Optional<Bson>,
    limit: OptionalInt
  ): Flux<ResultMap> {
    val publisher = getRawCollection(entityClass).find()
//    val includeFields = if (fields.contains(ID)) fields else fields.toMutableList().apply { add((ID)) }
    publisher.projection(Projections.include(fields))
    where.forEach { publisher.filter(it) }
    order.forEach { publisher.sort(it) }
    limit.forEach { publisher.limit(it) }
    return Flux.from(publisher).map { doc ->
      if (doc.containsKey(ID) && !doc.containsKey("id")) {
        doc["id"] = doc[ID]
      }
      ResultMap(doc)
    }
  }

  override fun <ID, E : Entity<ID>> queryIds(entityClass: Class<E>): Flux<ID> {
    val idType = EntityHelper.getIdType(entityClass)
    val publisher = getRawCollection(entityClass).find().projection(Projections.include(ID))
    return Flux.from(publisher).map { convertToID<ID>(it[ID]!!, idType) }
  }

  private fun <ID> convertToID(obj: Any, idType: Type): ID {
    if (idType is Class<*> && idType.isInstance(obj)) {
      return obj.unsafeCast()
    }
    return Json.convert(obj, idType)
  }

  override fun runCommand(cmd: Bson): Flux<Document> {
    return Flux.from(db.runCommand(cmd))
  }
}
