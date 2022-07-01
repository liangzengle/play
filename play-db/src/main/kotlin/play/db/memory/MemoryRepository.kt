package play.db.memory

import com.fasterxml.jackson.databind.JsonNode
import play.db.Repository
import play.db.ResultMap
import play.entity.Entity
import play.util.collection.toImmutableList
import play.util.json.Json
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

  override fun insert(entity: Entity<*>): Mono<Void> {
    val prev = getMap(entity.javaClass).putIfAbsent(entity.id(), entity)
    if (prev != null) {
      throw IllegalStateException("${entity.javaClass.simpleName}(${entity.id()}) already exists")
    }
    return Mono.empty()
  }

  override fun update(entity: Entity<*>): Mono<Void> {
    return Mono.empty()
  }

  override fun <ID, E : Entity<ID>> update(entityClass: Class<E>, id: ID, field: String, value: Any): Mono<Void> {
    return Mono.empty()
  }

  override fun insertOrUpdate(entity: Entity<*>): Mono<Void> {
    getMap(entity.javaClass).put(entity.id(), entity)
    return Mono.empty()
  }

  override fun delete(entity: Entity<*>): Mono<Void> {
    getMap(entity.javaClass).remove(entity.id())
    return Mono.empty()
  }

  override fun <ID, E : Entity<ID>> deleteById(id: ID, entityClass: Class<E>): Mono<Void> {
    getMap(entityClass).remove(id)
    return Mono.empty()
  }

  override fun batchInsertOrUpdate(entities: Collection<Entity<*>>): Mono<Void> {
    entities.forEach(::insertOrUpdate)
    return Mono.empty()
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

  override fun <IDX, ID : Any, E : Entity<ID>> loadIdsByIndex(
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
        var field = ""
        val iterator = indexName.splitToSequence('.').iterator()
        while (iterator.hasNext()) {
          field = iterator.next()
          val value = Reflect.getFieldValue<Any>(obj.javaClass.getDeclaredField(field), obj)
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
}
