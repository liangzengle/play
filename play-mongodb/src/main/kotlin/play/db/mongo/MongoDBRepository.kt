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
import play.entity.IntIdEntity
import play.entity.LongIdEntity
import play.util.*
import play.util.concurrent.Future
import play.util.concurrent.Promise
import play.util.json.Json
import play.util.reflect.Reflect
import java.util.*
import javax.annotation.CheckReturnValue

class MongoDBRepository constructor(
  dbName: String,
  private val tableNameResolver: TableNameResolver,
  client: MongoClient
) : Repository, MongoQueryService, MongoDBCommandSupport {

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

  override fun <ID, E : Entity<ID>> deleteIfDeleted(id: ID, entityClass: Class<E>): Future<out Any> {
    val promise = Promise.make<DeleteResult>()
    val filter = Filters.and(Filters.eq(id), Filters.eq(Entity.DELETED, true))
    getCollection(entityClass).deleteOne(filter, deleteOptions)
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

  override fun <ID, E : Entity<ID>> update(entityClass: Class<E>, id: ID, field: String, value: Any) {
    getCollection(entityClass).updateOne(Filters.eq(ID, id), Updates.set(field, value))
  }

  override fun <ID, E : Entity<ID>, R, R1 : R> fold(entityClass: Class<E>, initial: R1, f: (R1, E) -> R1): Future<R> {
    return fold(entityClass, empty(), empty(), empty(), initial, f)
  }

  override fun <ID, E : Entity<ID>, R, R1 : R> fold(
    entityClass: Class<E>,
    fields: List<String>,
    initial: R1,
    folder: (R1, ResultMap) -> R1
  ): Future<R> {
    return fold(entityClass, fields, empty(), empty(), empty(), initial, folder)
  }

  override fun <ID, E : Entity<ID>> findById(entityClass: Class<E>, id: ID): Future<Optional<E>> {
    val promise = Promise.make<E?>()
    getCollection(entityClass).find(Filters.eq(id)).subscribe(NullableForOneSubscriber(promise))
    return promise.future.map { it.toOptional() }
  }

  override fun <ID, E : Entity<ID>> loadAll(ids: Iterable<ID>, entityClass: Class<E>): Future<List<E>> {
    val promise = Promise.make<List<E>>()
    getCollection(entityClass).find(Filters.`in`(ID, ids))
      .subscribe(FoldSubscriber(promise, LinkedList()) { list, e -> list.add(e); list })
    return promise.future
  }

  override fun <ID, E : Entity<ID>> listAll(entityClass: Class<E>): Future<List<E>> {
    val promise = Promise.make<List<E>>()
    getCollection(entityClass).find().subscribe(FoldSubscriber(promise, LinkedList()) { list, e -> list.add(e); list })
    return promise.future
  }

  @CheckReturnValue
  override fun <ID, E : Entity<ID>, R, R1 : R> fold(
    entityClass: Class<E>,
    where: Optional<Bson>,
    order: Optional<Bson>,
    limit: Optional<Int>,
    initial: R1,
    folder: (R1, E) -> R1
  ): Future<R> {
    val promise = Promise.make<R>()
    val publisher = getCollection(entityClass).find()
    where.forEach { publisher.filter(it) }
    order.forEach { publisher.sort(it) }
    limit.forEach { publisher.limit(it) }
    publisher.subscribe(FoldSubscriber(promise, initial, folder))
    return promise.future
  }

  @CheckReturnValue
  override fun <ID, E : Entity<ID>, R, R1 : R> fold(
    entityClass: Class<E>,
    fields: List<String>,
    where: Optional<Bson>,
    order: Optional<Bson>,
    limit: Optional<Int>,
    initial: R1,
    folder: (R1, ResultMap) -> R1
  ): Future<R> {
    val promise = Promise.make<R>()
    val publisher = getRawCollection(entityClass).find()
    val includeFields = if (fields.contains(ID)) fields else fields.toMutableList().apply { add((ID)) }
    publisher.projection(Projections.include(includeFields))
    where.forEach { publisher.filter(it) }
    order.forEach { publisher.sort(it) }
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
    val idType = getIdType<ID>(entityClass)
    getRawCollection(entityClass)
      .find()
      .projection(Projections.include(ID))
      .subscribe(
        FoldSubscriber(
          promise,
          LinkedList()
        ) { list, doc ->
          val id = convertToID(doc[ID]!!, idType)
          list.add(id)
          list
        }
      )
    return promise.future
  }

  private fun <ID> getIdType(entityClass: Class<out Entity<*>>): Class<ID> {
    if (isAssignableFrom<LongIdEntity>(entityClass)) {
      return Long::class.java.unsafeCast()
    }
    if (isAssignableFrom<IntIdEntity>(entityClass)) {
      return Int::class.java.unsafeCast()
    }
    return Reflect.getRawClass<Any>(Reflect.getTypeArg(entityClass, Entity::class.java, 0)).unsafeCast()
  }

  private fun <ID> convertToID(obj: Any, idType: Class<ID>): ID {
    if (idType.isInstance(obj)) {
      return obj.unsafeCast()
    }
    return Json.convert(obj, idType)
  }

  override fun runCommand(cmd: Bson): Future<Document> {
    val promise = Promise.make<Document>()
    db.runCommand(cmd).subscribe(ForOneSubscriber(promise))
    return promise.future
  }
}
