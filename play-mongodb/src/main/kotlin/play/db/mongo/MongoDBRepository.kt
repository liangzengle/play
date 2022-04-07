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
import org.reactivestreams.FlowAdapters
import play.db.Repository
import play.db.ResultMap
import play.db.TableNameResolver
import play.db.mongo.Mongo.ID
import play.entity.Entity
import play.entity.EntityHelper
import play.entity.ObjId
import play.entity.ObjIdEntity
import play.util.*
import play.util.concurrent.*
import play.util.json.Json
import java.lang.reflect.Type
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

  fun listCollectionNames(): Future<MutableSet<String>> {
    return FlowAdapters.toFlowPublisher(db.listCollectionNames()).subscribeToCollection(hashSetOf())
  }

  internal fun <T : Entity<*>> createCollection(clazz: Class<T>): Future<*> {
    return FlowAdapters.toFlowPublisher(db.createCollection(getCollectionName(clazz))).subscribeOneNullable()
  }

  private fun getRawCollection(clazz: Class<*>): MongoCollection<Document> {
    return db.getCollection(tableNameResolver.resolve(clazz))
  }

  override fun insert(entity: Entity<*>): Future<InsertOneResult> {
    val publisher = getCollection(entity).insertOne(entity, insertOneOptions)
    return FlowAdapters.toFlowPublisher(publisher).subscribeOne()
  }

  override fun update(entity: Entity<*>): Future<UpdateResult> {
    return insertOrUpdate(entity)
  }

  override fun insertOrUpdate(entity: Entity<*>): Future<UpdateResult> {
    val publisher = getCollection(entity).replaceOne(Filters.eq(entity.id()), entity, replaceOptions)
    return FlowAdapters.toFlowPublisher(publisher).subscribeOne()
  }

  override fun delete(entity: Entity<*>): Future<DeleteResult> {
    return deleteById(entity.id(), entity.javaClass.unsafeCast())
  }

  override fun <ID, E : Entity<ID>> deleteById(id: ID, entityClass: Class<E>): Future<DeleteResult> {
    val publisher = getCollection(entityClass).deleteOne(Filters.eq(id), deleteOptions)
    return FlowAdapters.toFlowPublisher(publisher).subscribeOne()
  }

  override fun batchInsertOrUpdate(entities: Collection<Entity<*>>): Future<BulkWriteResult> {
    if (entities.isEmpty()) {
      return Future.successful(BulkWriteResult.acknowledged(0, 0, 0, 0, emptyList(), emptyList()))
    }
    // mongo driver will divide into small groups when a group exceeds the limit, which is 100,000 in MongoDB 3.6
    val writeModels = entities.map { ReplaceOneModel(Filters.eq(it.id()), it, replaceOptions) }
    val publisher = getCollection(entities.first().javaClass).bulkWrite(writeModels, unorderedBulkWrite)
    return FlowAdapters.toFlowPublisher(publisher).subscribeOne()
  }

  override fun <ID, E : Entity<ID>> update(
    entityClass: Class<E>,
    id: ID,
    field: String,
    value: Any
  ): Future<UpdateResult> {
    val publisher = getCollection(entityClass).updateOne(Filters.eq(ID, id), Updates.set(field, value))
    return FlowAdapters.toFlowPublisher(publisher).subscribeOne()
  }

  override fun <ID, E : Entity<ID>, R, R1 : R> fold(entityClass: Class<E>, initial: R1, f: (R1, E) -> R1): Future<R> {
    return fold(entityClass, empty(), empty(), emptyInt(), initial, f)
  }

  override fun <ID, E : Entity<ID>, R, R1 : R> fold(
    entityClass: Class<E>,
    fields: List<String>,
    initial: R1,
    folder: (R1, ResultMap) -> R1
  ): Future<R> {
    return fold(entityClass, fields, empty(), empty(), emptyInt(), initial, folder)
  }

  override fun <ID, E : Entity<ID>> findById(entityClass: Class<E>, id: ID): Future<Optional<E>> {
    val publisher = getCollection(entityClass).find(Filters.eq(id))
    return FlowAdapters.toFlowPublisher(publisher).subscribeOneOptional()
  }

  override fun <ID, E : Entity<ID>> loadAll(ids: Iterable<ID>, entityClass: Class<E>): Future<List<E>> {
    val publisher = getCollection(entityClass).find(Filters.`in`(ID, ids))
    return FlowAdapters.toFlowPublisher(publisher).subscribeToList()
  }

  override fun <K, ID : ObjId, E : ObjIdEntity<ID>> listMultiIds(
    entityClass: Class<E>,
    keyName: String,
    keyValue: K
  ): Future<List<ID>> {
    val idType = EntityHelper.getIdType(entityClass)
    val where = Filters.and(Filters.eq("$ID.$keyName", keyValue), Filters.ne(Entity.DELETED, true))
    return fold(
      entityClass,
      listOf(ID),
      where.toOptional(),
      empty(),
      emptyInt(),
      LinkedList<ID>()
    ) { list, r ->
      val id = convertToID<ID>(r.getObject(ID), idType)
      list.add(id)
      list
    }
  }

  override fun <ID, E : Entity<ID>> listAll(entityClass: Class<E>): Future<List<E>> {
    val publisher = getCollection(entityClass).find()
    return FlowAdapters.toFlowPublisher(publisher).subscribeToList()
  }

  @CheckReturnValue
  override fun <ID, E : Entity<ID>, R, R1 : R> fold(
    entityClass: Class<E>,
    where: Optional<Bson>,
    order: Optional<Bson>,
    limit: OptionalInt,
    initial: R1,
    folder: (R1, E) -> R1
  ): Future<R> {
    val publisher = getCollection(entityClass).find()
    where.forEach { publisher.filter(it) }
    order.forEach { publisher.sort(it) }
    limit.forEach { publisher.limit(it) }
    return FlowAdapters.toFlowPublisher(publisher).subscribeInto(initial, folder)
  }

  @CheckReturnValue
  override fun <ID, E : Entity<ID>, R, R1 : R> fold(
    entityClass: Class<E>,
    fields: List<String>,
    where: Optional<Bson>,
    order: Optional<Bson>,
    limit: OptionalInt,
    initial: R1,
    folder: (R1, ResultMap) -> R1
  ): Future<R> {
    val publisher = getRawCollection(entityClass).find()
    val includeFields = if (fields.contains(ID)) fields else fields.toMutableList().apply { add((ID)) }
    publisher.projection(Projections.include(includeFields))
    where.forEach { publisher.filter(it) }
    order.forEach { publisher.sort(it) }
    limit.forEach { publisher.limit(it) }
    return FlowAdapters.toFlowPublisher(publisher).subscribeInto(initial) { r, doc ->
      if (doc.containsKey(ID) && !doc.containsKey("id")) {
        doc["id"] = doc[ID]
      }
      folder(r, ResultMap(doc))
    }
  }

  override fun <ID, E : Entity<ID>> listIds(entityClass: Class<E>): Future<List<ID>> {
    return collectId(entityClass, LinkedList<ID>()) { list, id ->
      list.add(id)
      list
    }
  }

  override fun <ID, E : Entity<ID>, C, C1 : C> collectId(
    entityClass: Class<E>,
    c: C1,
    accumulator: (C1, ID) -> C1
  ): Future<C> {
    val idType = EntityHelper.getIdType(entityClass)
    val publisher = getRawCollection(entityClass).find().projection(Projections.include(ID))
    return FlowAdapters.toFlowPublisher(publisher).subscribeInto(c) { list, doc ->
      val id = convertToID<ID>(doc[ID]!!, idType)
      accumulator(list, id)
    }
  }

  private fun <ID> convertToID(obj: Any, idType: Type): ID {
    if (idType is Class<*> && idType.isInstance(obj)) {
      return obj.unsafeCast()
    }
    return Json.convert(obj, idType)
  }

  override fun runCommand(cmd: Bson): Future<Document> {
    return FlowAdapters.toFlowPublisher(db.runCommand(cmd)).subscribeOne()
  }
}
