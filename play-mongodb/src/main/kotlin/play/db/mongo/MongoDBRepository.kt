package play.db.mongo

import com.mongodb.bulk.BulkWriteResult
import com.mongodb.client.model.*
import com.mongodb.client.result.DeleteResult
import com.mongodb.client.result.InsertOneResult
import com.mongodb.client.result.UpdateResult
import com.mongodb.reactivestreams.client.MongoClient
import com.mongodb.reactivestreams.client.MongoCollection
import org.bson.BsonDocument
import org.bson.Document
import org.bson.conversions.Bson
import play.db.Repository
import play.db.ResultMap
import play.db.TableNameResolver
import play.db.mongo.Mongo.ID
import play.entity.Entity
import play.util.concurrent.Future
import play.util.concurrent.Promise
import play.util.forEach
import play.util.json.Json
import play.util.reflect.Reflect
import play.util.toOptional
import play.util.unsafeCast
import java.util.*
import javax.annotation.CheckReturnValue

class MongoDBRepository constructor(
  dbName: String,
  private val tableNameResolver: TableNameResolver,
  client: MongoClient
) : Repository, MongoDBCommandSupport {

  private val db = client.getDatabase(dbName)

  private val unorderedBulkWrite = BulkWriteOptions().ordered(false)
  private val insertOneOptions = InsertOneOptions()
  private val updateOptions = UpdateOptions().upsert(true)
  private val replaceOptions = ReplaceOptions().upsert(true)
  private val deleteOptions = DeleteOptions()

  private fun <T : Entity<*>> getCollection(entity: T): MongoCollection<T> {
    return getCollection(entity.javaClass)
  }

  internal fun <T : Entity<*>> getCollection(clazz: Class<T>): MongoCollection<T> {
    return db.getCollection(tableNameResolver.resolve(clazz), clazz)
  }

  private fun getRawCollection(clazz: Class<*>): MongoCollection<Document> {
    return db.getCollection(tableNameResolver.resolve(clazz))
  }

  override fun insert(entity: Entity<*>): Future<InsertOneResult> {
    val promise = Promise.make<InsertOneResult>()
    getCollection(entity).insertOne(entity, insertOneOptions).subscribe(ForOneSubscriber(promise))
    return promise.future
  }

  override fun update(entity: Entity<*>): Future<UpdateResult> {
    val promise = Promise.make<UpdateResult>()
    getCollection(entity).replaceOne(Filters.eq(entity.id()), entity, replaceOptions)
      .subscribe(ForOneSubscriber(promise))
    return promise.future
  }

  override fun insertOrUpdate(entity: Entity<*>): Future<UpdateResult> {
    val promise = Promise.make<UpdateResult>()
    getCollection(entity).replaceOne(Filters.eq(entity.id()), entity, replaceOptions)
      .subscribe(ForOneSubscriber(promise))
    return promise.future
  }

  override fun delete(entity: Entity<*>): Future<DeleteResult> {
    return deleteById(entity.id(), entity.javaClass.unsafeCast())
  }

  override fun <ID, E : Entity<ID>> deleteById(id: ID, entityClass: Class<E>): Future<DeleteResult> {
    val promise = Promise.make<DeleteResult>()
    getCollection(entityClass).deleteOne(Filters.eq(id), deleteOptions)
      .subscribe(ForOneSubscriber(promise))
    return promise.future
  }

  override fun batchInsertOrUpdate(entities: Collection<Entity<*>>): Future<BulkWriteResult> {
    if (entities.isEmpty()) {
      return Future.successful(BulkWriteResult.acknowledged(0, 0, 0, 0, emptyList(), emptyList()))
    }
    // mongo driver will divide into small groups when a group exceeds the limit, which is 100,000 in MongoDB 3.6
    val promise = Promise.make<BulkWriteResult>()
    val writeModels = entities.map { ReplaceOneModel(Filters.eq(it.id()), it, replaceOptions) }
    getCollection(entities.first().javaClass).bulkWrite(writeModels, unorderedBulkWrite)
      .subscribe(ForOneSubscriber(promise))
    return promise.future
  }

  override fun <ID, E : Entity<ID>> findById(id: ID, entityClass: Class<E>): Future<Optional<E>> {
    val promise = Promise.make<E?>()
    getCollection(entityClass).find(Filters.eq(id)).subscribe(NullableForOneSubscriber(promise))
    return promise.future.map { it.toOptional() }
  }

  override fun <ID, E : Entity<ID>> listAll(entityClass: Class<E>): Future<List<E>> {
    val promise = Promise.make<List<E>>()
    getCollection(entityClass).find().subscribe(FoldSubscriber(promise, LinkedList()) { list, e -> list.add(e); list })
    return promise.future
  }

  @CheckReturnValue
  override fun <ID, E : Entity<ID>, R, R1 : R> fold(
    entityClass: Class<E>,
    where: Optional<String>,
    order: Optional<String>,
    limit: Optional<Int>,
    initial: R1,
    folder: (R1, E) -> R1
  ): Future<R> {
    val promise = Promise.make<R>()
    val publisher = getCollection(entityClass).find()
    where.forEach { publisher.filter(BsonDocument.parse(it)) }
    order.forEach { publisher.sort(BsonDocument.parse(it)) }
    limit.forEach { publisher.limit(it) }
    publisher.subscribe(FoldSubscriber(promise, initial, folder))
    return promise.future
  }

  @CheckReturnValue
  override fun <ID, E : Entity<ID>, R, R1 : R> fold(
    entityClass: Class<E>,
    fields: List<String>,
    where: Optional<String>,
    order: Optional<String>,
    limit: Optional<Int>,
    initial: R1,
    folder: (R1, ResultMap) -> R1
  ): Future<R> {
    val promise = Promise.make<R>()
    val publisher = getRawCollection(entityClass).find()
    val includeFields = if (fields.contains(ID)) fields else fields.toMutableList().apply { add((ID)) }
    publisher.projection(Projections.include(includeFields))
    where.forEach { publisher.filter(BsonDocument.parse(it)) }
    order.forEach { publisher.sort(BsonDocument.parse(it)) }
    limit.forEach { publisher.limit(it) }
    publisher.subscribe(FoldSubscriber(promise, initial) { r, doc ->
      if (doc.containsKey(ID) && !doc.containsKey("id")) {
        doc["id"] = doc[ID]
      }
      folder(r, ResultMap(doc))
    })
    return promise.future
  }

  override fun <ID, E : Entity<ID>> listIds(entityClass: Class<E>): Future<List<ID>> {
    val promise = Promise.make<List<ID>>()
    getRawCollection(entityClass)
      .find()
      .projection(Projections.include(ID))
      .subscribe(
        FoldSubscriber(
          promise,
          LinkedList()
        ) { list, doc ->
          val id = convertToID<ID>(doc[ID]!!, entityClass)
          list.add(id)
          list
        }
      )
    return promise.future
  }

  private fun getIdType(entityClass: Class<out Entity<*>>): Class<*> {
    return Reflect.getRawClass<Any>(Reflect.getTypeArg(entityClass, Entity::class.java, 0))
  }

  private fun <ID> convertToID(obj: Any, entityClass: Class<out Entity<*>>): ID {
    if (obj !is Document) {
      return obj.unsafeCast()
    }
    val idType = getIdType(entityClass)
    if (idType.isAssignableFrom(obj.javaClass)) {
      return obj.unsafeCast()
    }
    return Json.convert(obj, idType).unsafeCast()
  }

  override fun runCommand(cmd: Bson): Future<Document> {
    val promise = Promise.make<Document>()
    db.runCommand(cmd).subscribe(ForOneSubscriber(promise))
    return promise.future
  }
}
