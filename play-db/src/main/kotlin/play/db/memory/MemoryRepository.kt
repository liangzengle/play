package play.db.memory

import com.fasterxml.jackson.databind.JsonNode
import play.db.Repository
import play.db.ResultMap
import play.entity.Entity
import play.entity.EntityHelper
import play.entity.ObjId
import play.entity.cache.CacheIndex
import play.util.collection.toImmutableList
import play.util.concurrent.Future
import play.util.isAssignableFrom
import play.util.json.Json
import play.util.mkString
import play.util.reflect.Reflect
import play.util.unsafeCast
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import kotlin.streams.asStream

@Suppress("UNCHECKED_CAST")
class MemoryRepository : Repository {
  private val caches: ConcurrentMap<Class<*>, ConcurrentMap<Any, Entity<*>>> = ConcurrentHashMap()

  private fun getMap(entityClass: Class<*>): ConcurrentMap<Any, Entity<*>> {
    val map = caches[entityClass]
    return map ?: caches.computeIfAbsent(entityClass) { ConcurrentHashMap() }
  }

  override fun insert(entity: Entity<*>): Future<Unit> {
    val prev = getMap(entity.javaClass).putIfAbsent(entity.id(), entity)
    if (prev != null) {
      throw IllegalStateException("${entity.javaClass.simpleName}(${entity.id()}) already exists")
    }
    return Future.successful(Unit)
  }

  override fun update(entity: Entity<*>): Future<Unit> {
    return Future.successful(Unit)
  }

  override fun <ID, E : Entity<ID>> update(entityClass: Class<E>, id: ID, field: String, value: Any): Future<Unit> {
    return Future.successful(Unit)
  }

  override fun insertOrUpdate(entity: Entity<*>): Future<Unit> {
    getMap(entity.javaClass)[entity.id()] = entity
    return Future.successful(Unit)
  }

  override fun delete(entity: Entity<*>): Future<Unit> {
    getMap(entity.javaClass).remove(entity.id())
    return Future.successful(Unit)
  }

  override fun <ID, E : Entity<ID>> deleteById(id: ID, entityClass: Class<E>): Future<Unit> {
    getMap(entityClass).remove(id)
    return Future.successful(Unit)
  }

  override fun batchInsertOrUpdate(entities: Collection<Entity<*>>): Future<Unit> {
    entities.forEach(::insertOrUpdate)
    return Future.successful(Unit)
  }

  override fun <ID, E : Entity<ID>> loadAll(entityClass: Class<E>, ids: Iterable<ID>): Flux<E> {
    val map = getMap(entityClass)
    return Flux.fromStream(ids.asSequence().map { id -> map[id] as E? }.filterNotNull().asStream())
  }

  override fun <ID, E : Entity<ID>> findById(entityClass: Class<E>, id: ID): Mono<E> {
    val e = getMap(entityClass)[id] as E?
    return Mono.justOrEmpty(e)
  }

  override fun <ID, E : Entity<ID>> queryAll(entityClass: Class<E>): Flux<E> {
    return Flux.fromIterable(getMap(entityClass).values as Collection<E>)
  }

  override fun <ID, E : Entity<ID>> queryIds(entityClass: Class<E>): Flux<ID> {
    return Flux.fromIterable(getMap(entityClass).keys as Collection<ID>)
  }

  override fun <ID, E : Entity<ID>> query(entityClass: Class<E>, fields: List<String>): Flux<ResultMap> {
    return queryAll(entityClass).map { entity ->
      val jsonNode = Json.convert(entity, JsonNode::class.java)
      val resultMap = linkedMapOf<String, Any>()
      for (field in fields) {
        val value = jsonNode.get(field)
        if (value != null) {
          resultMap[field] = value
        }
      }
      resultMap["id"] = entity.id() as Any
      ResultMap(resultMap)
    }
  }

  override fun <IDX, ID, E : Entity<ID>> queryIdsByIndex(
    entityClass: Class<E>,
    indexName: String,
    indexValue: IDX
  ): Flux<ID> {
    val map = getMap(entityClass)
    if (map.isEmpty()) {
      return Flux.empty()
    }
    val result = map.values.asSequence()
      .map { it.unsafeCast<E>() }
      .filter { entity ->
        var match = false
        var obj: Any = entity
        var field: String
        val iterator = indexName.splitToSequence('.').iterator()
        while (iterator.hasNext()) {
          field = iterator.next()
          val value = Reflect.getFieldValue<Any>(Reflect.getField(obj.javaClass, field), obj)
          if (!iterator.hasNext()) {
            match = indexValue == value
            break
          }
          if (value == null) {
            break
          }
          obj = value
        }
        match
      }
      .map { it.id() }
      .toImmutableList()
    return Flux.fromIterable(result)
  }

  override fun <IDX, ID, E : Entity<ID>> loadIdsByCacheIndex(entityClass: Class<E>, indexValue: IDX): Flux<ID> {
    var indexName = getCacheIndexName(entityClass)
    if (indexName == null) {
      val idClass = EntityHelper.getIdClass(entityClass)
      if (isAssignableFrom<ObjId>(idClass)) {
        indexName = getCacheIndexName(idClass, listOf("id"))
      }
    }
    if (indexName == null) {
      throw IllegalArgumentException("CacheIndex not found in class: ${entityClass.name}")
    }
    return queryIdsByIndex(entityClass, indexName, indexValue)
  }

  private fun getCacheIndexName(clazz: Class<*>, parents: List<String> = emptyList()): String? {
    return clazz.declaredFields.asSequence()
      .firstOrNull { it.isAnnotationPresent(CacheIndex::class.java) }
      ?.let { field ->
        val indexFieldName = if (parents.isEmpty()) {
          field.name
        } else {
          parents.mkString('.', "", '.') + field.name
        }
        indexFieldName
      }
  }
}
