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
import io.vavr.concurrent.Future
import io.vavr.concurrent.Promise
import io.vavr.control.Option
import org.bson.BsonDocument
import org.bson.Document
import play.Configuration
import play.db.Entity
import play.db.Repository
import play.db.ResultMap
import play.db.TableNameResolver
import play.util.unsafeCast
import java.util.*
import javax.annotation.CheckReturnValue
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class MongoDBRespository @Inject constructor(
  private val tableNameResolver: TableNameResolver,
  setting: MongoClientSettings,
  @Named("mongodb") conf: Configuration
) : Repository {

  private val client = MongoClients.create(setting)

  private val db = client.getDatabase(conf.getString("db"))

  private val unorderedBulkWrite = BulkWriteOptions().ordered(false)
  private val insertOneOptions = InsertOneOptions()
  private val upsertUpdate = UpdateOptions().upsert(true)
  private val upsertReplace = ReplaceOptions().upsert(true)
  private val deleteOptions = DeleteOptions()

  private fun <T : Entity<*>> collectionOf(entity: T): MongoCollection<T> {
    return collectionOf(entity.javaClass)
  }

  private fun <T : Entity<*>> collectionOf(clazz: Class<T>): MongoCollection<T> {
    return db.getCollection(tableNameResolver.resolve(clazz), clazz)
  }

  override fun insert(entity: Entity<*>): Future<InsertOneResult> {
    val promise = Promise.make<InsertOneResult>()
    collectionOf(entity).insertOne(entity, insertOneOptions).subscribe(ForOneSubscriber(promise))
    return promise.future()
  }

  override fun update(entity: Entity<*>): Future<UpdateResult> {
    val promise = Promise.make<UpdateResult>()
    collectionOf(entity).replaceOne(Filters.eq(entity.id()), entity)
      .subscribe(ForOneSubscriber(promise))
    return promise.future()
  }

  override fun insertOrUpdate(entity: Entity<*>): Future<UpdateResult> {
    val promise = Promise.make<UpdateResult>()
    collectionOf(entity).replaceOne(Filters.eq(entity.id()), entity, upsertReplace)
      .subscribe(ForOneSubscriber(promise))
    return promise.future()
  }

  override fun delete(entity: Entity<*>): Future<DeleteResult> {
    return deleteById(entity.id(), entity.javaClass.unsafeCast())
  }

  override fun <ID, E : Entity<ID>> deleteById(id: ID, entityClass: Class<E>): Future<DeleteResult> {
    val promise = Promise.make<DeleteResult>()
    collectionOf(entityClass).deleteOne(Filters.eq(id), deleteOptions)
      .subscribe(ForOneSubscriber(promise))
    return promise.future()
  }

  override fun batchInsertOrUpdate(entities: Collection<Entity<*>>): Future<BulkWriteResult> {
    if (entities.isEmpty()) {
      return Future.successful(BulkWriteResult.acknowledged(0, 0, 0, null, emptyList(), emptyList()))
    }
    val promise = Promise.make<BulkWriteResult>()
    val writeModules = entities.map { ReplaceOneModel(Filters.eq(it.id()), it, upsertReplace) }
    collectionOf(entities.first().javaClass).bulkWrite(writeModules, unorderedBulkWrite)
      .subscribe(ForOneSubscriber(promise))
    return promise.future()
  }

  override fun <ID, E : Entity<ID>> findById(id: ID, entityClass: Class<E>): Future<Option<E>> {
    val promise = Promise.make<E>()
    collectionOf(entityClass).find(Filters.eq(id)).subscribe(ForOneSubscriber(promise))
    return promise.future().map { Option.of(it) }
  }

  override fun <ID, E : Entity<ID>> listAll(entityClass: Class<E>): Future<List<E>> {
    val promise = Promise.make<List<E>>()
    collectionOf(entityClass).find().subscribe(FoldSubscriber(promise, LinkedList()) { list, e -> list.add(e); list })
    return promise.future()
  }

  @CheckReturnValue
  override fun <ID, E : Entity<ID>, R> fold(entityClass: Class<E>, initial: R, f: (R, E) -> R): Future<R> {
    val promise = Promise.make<R>()
    collectionOf(entityClass).find().subscribe(FoldSubscriber(promise, initial, f))
    return promise.future()
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
    val publisher = collectionOf(entityClass).find()
    publisher.projection(Projections.include(fields))
    where.forEach { publisher.filter(BsonDocument.parse(it)) }
    order.forEach { publisher.sort(BsonDocument.parse(it)) }
    limit.forEach { publisher.limit(it) }
    publisher.unsafeCast<FindPublisher<Document>>()
      .subscribe(FoldSubscriber(promise, initial, { r, t -> folder(r, ResultMap(t)) }))
    return promise.future()
  }

  override fun <ID, E : Entity<ID>> listIds(entityClass: Class<E>): Future<List<ID>> {
    val promise = Promise.make<List<ID>>()
    collectionOf(entityClass)
      .unsafeCast<MongoCollection<Document>>()
      .find()
      .projection(Projections.include("_id"))
      .subscribe(FoldSubscriber(promise, LinkedList(), { list, doc ->
        val id: ID = doc["_id"]?.unsafeCast()!!
        list.add(id)
        list
      }))
    return promise.future()
  }

  override fun <ID, E : Entity<ID>> query(
    entityClass: Class<E>,
    where: Option<String>,
    order: Option<String>,
    limit: Option<Int>
  ): Future<List<E>> {
    val promise = Promise.make<List<E>>()
    val publisher = collectionOf(entityClass).find()
    where.forEach { publisher.filter(BsonDocument.parse(it)) }
    order.forEach { publisher.sort(BsonDocument.parse(it)) }
    limit.forEach { publisher.limit(it) }
    publisher.subscribe(FoldSubscriber(promise, LinkedList()) { list, e -> list.add(e); list })
    return promise.future()
  }

  override fun <ID, E : Entity<ID>> query(
    entityClass: Class<E>,
    fields: List<String>,
    where: Option<String>,
    order: Option<String>,
    limit: Option<Int>
  ): Future<List<ResultMap>> {
    val promise = Promise.make<List<ResultMap>>()
    val publisher = collectionOf(entityClass).find()
    where.forEach { publisher.filter(BsonDocument.parse(it)) }
    order.forEach { publisher.sort(BsonDocument.parse(it)) }
    limit.forEach { publisher.limit(it) }
    publisher.unsafeCast<FindPublisher<Document>>()
      .subscribe(FoldSubscriber(promise, LinkedList()) { list, doc -> list.add(ResultMap(doc)); list })
    return promise.future()
  }

  override fun close() {
    client.close()
  }
}
