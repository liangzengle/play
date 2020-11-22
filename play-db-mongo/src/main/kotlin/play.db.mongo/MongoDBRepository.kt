package play.db.mongo

import com.mongodb.MongoClientSettings
import com.mongodb.bulk.BulkWriteResult
import com.mongodb.client.model.*
import com.mongodb.client.result.DeleteResult
import com.mongodb.client.result.InsertOneResult
import com.mongodb.client.result.UpdateResult
import com.mongodb.reactivestreams.client.FindPublisher
import com.mongodb.reactivestreams.client.MongoClients
import com.mongodb.reactivestreams.client.MongoCollection
import org.bson.BsonDocument
import org.bson.Document
import play.ApplicationLifecycle
import play.Configuration
import play.db.Entity
import play.db.Repository
import play.db.ResultMap
import play.db.TableNameResolver
import play.util.concurrent.Future
import play.util.concurrent.Promise
import play.util.forEach
import play.util.toOptional
import play.util.unsafeCast
import java.util.*
import javax.annotation.CheckReturnValue
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class MongoDBRepository @Inject constructor(
  private val tableNameResolver: TableNameResolver,
  setting: MongoClientSettings,
  @Named("mongodb") conf: Configuration,
  lifecycle: ApplicationLifecycle
) : Repository(lifecycle) {

  private val client = MongoClients.create(setting)

  private val db = client.getDatabase(conf.getString("db"))

  private val unorderedBulkWrite = BulkWriteOptions().ordered(false)
  private val insertOneOptions = InsertOneOptions()
  private val updateOptions = UpdateOptions().upsert(true)
  private val replaceOptions = ReplaceOptions().upsert(true)
  private val deleteOptions = DeleteOptions()

  private fun <T : Entity<*>> getCollection(entity: T): MongoCollection<T> {
    return getCollection(entity.javaClass)
  }

  private fun <T : Entity<*>> getCollection(clazz: Class<T>): MongoCollection<T> {
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
  override fun <ID, E : Entity<ID>, R> fold(entityClass: Class<E>, initial: R, f: (R, E) -> R): Future<R> {
    val promise = Promise.make<R>()
    getCollection(entityClass).find().subscribe(FoldSubscriber(promise, initial, f))
    return promise.future
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
    val publisher = getCollection(entityClass).find()
    publisher.projection(Projections.include(fields))
    where.forEach { publisher.filter(BsonDocument.parse(it)) }
    order.forEach { publisher.sort(BsonDocument.parse(it)) }
    limit.forEach { publisher.limit(it) }
    publisher.unsafeCast<FindPublisher<Document>>()
      .subscribe(FoldSubscriber(promise, initial, { r, t -> folder(r, ResultMap(t)) }))
    return promise.future
  }

  override fun <ID, E : Entity<ID>> listIds(entityClass: Class<E>): Future<List<ID>> {
    val promise = Promise.make<List<ID>>()
    getRawCollection(entityClass)
      .find()
      .projection(Projections.include("_id"))
      .subscribe(
        FoldSubscriber(
          promise,
          LinkedList(),
          { list, doc ->
            val id: ID = doc["_id"]?.unsafeCast()!!
            list.add(id)
            list
          }
        )
      )
    return promise.future
  }

  override fun <ID, E : Entity<ID>> query(
    entityClass: Class<E>,
    where: Optional<String>,
    order: Optional<String>,
    limit: Optional<Int>
  ): Future<List<E>> {
    val promise = Promise.make<List<E>>()
    val publisher = getCollection(entityClass).find()
    where.forEach { publisher.filter(BsonDocument.parse(it)) }
    order.forEach { publisher.sort(BsonDocument.parse(it)) }
    limit.forEach { publisher.limit(it) }
    publisher.subscribe(FoldSubscriber(promise, LinkedList()) { list, e -> list.add(e); list })
    return promise.future
  }

  override fun <ID, E : Entity<ID>> query(
    entityClass: Class<E>,
    fields: List<String>,
    where: Optional<String>,
    order: Optional<String>,
    limit: Optional<Int>
  ): Future<List<ResultMap>> {
    val promise = Promise.make<List<ResultMap>>()
    val publisher = getRawCollection(entityClass).find()
    where.forEach { publisher.filter(BsonDocument.parse(it)) }
    order.forEach { publisher.sort(BsonDocument.parse(it)) }
    limit.forEach { publisher.limit(it) }
    publisher.unsafeCast<FindPublisher<Document>>()
      .subscribe(FoldSubscriber(promise, LinkedList()) { list, doc -> list.add(ResultMap(doc)); list })
    return promise.future
  }

  override fun close() {
    client.close()
  }
}
