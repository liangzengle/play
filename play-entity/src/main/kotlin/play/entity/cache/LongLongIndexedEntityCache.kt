package play.entity.cache

import org.eclipse.collections.impl.factory.primitive.LongLists
import play.entity.LongIdEntity
import play.scheduling.Scheduler
import play.util.collection.ConcurrentHashSetLong
import play.util.collection.ConcurrentLongObjectMap
import play.util.getOrNull
import play.util.max
import play.util.min
import java.time.Duration
import java.util.function.ToLongFunction

/**
 * Long id, Long index
 */
class LongLongIndexedEntityCache<E : LongIdEntity>(
  private val indexName: String,
  private val indexMapper: ToLongFunction<E>,
  private val entityCache: EntityCacheLong<E>,
  private val entityCacheLoader: EntityCacheLoader,
  scheduler: Scheduler,
  keepAlive: Duration
) : IndexedEntityCache<Long, Long, E>, EntityCacheLong<E> by entityCache, EntityCacheInternalApi<E> {

  private val cache = ConcurrentLongObjectMap<ConcurrentHashSetLong>()

  init {
    require(entityCache is EntityCacheInternalApi<*>) { "${entityCache.javaClass} must implement EntityCacheInternalApi" }
    if (EntityCacheHelper.isNeverExpire(entityClass)) {
      val interval = Duration.ZERO max (keepAlive.dividedBy(2) min keepAlive)
      require(interval > Duration.ZERO) { "interval must be positive" }
      scheduler.scheduleWithFixedDelay(interval, ::evict)
    }
  }

  private fun evict() {
    fun canExpire(index: Long): Boolean {
      val expireEvaluator = expireEvaluator()
      for (id in getIds(index)) {
        val entity = entityCache.getCached(id).getOrNull() ?: continue
        if (!expireEvaluator.canExpire(entity)) {
          return false
        }
      }
      return true
    }

    val toBeEvict = LongLists.mutable.empty()
    for (entry in cache) {
      val index = entry.key
      val value = entry.value
      if (value.isEmpty() || canExpire(index)) {
        toBeEvict.add(index)
      }
    }
    for (i in 0 until toBeEvict.size()) {
      val index = toBeEvict.get(i)
      cache.computeIfPresent(index) { k, v ->
        if (v.isEmpty() || canExpire(k)) {
          null
        } else {
          v
        }
      }
    }
  }

  @Suppress("UNCHECKED_CAST")
  override fun expireEvaluator(): ExpireEvaluator {
    return (entityCache as EntityCacheInternalApi<E>).expireEvaluator()
  }

  @Suppress("UNCHECKED_CAST")
  override fun getAllCached(): Sequence<E> {
    return (entityCache as EntityCacheInternalApi<E>).getAllCached()
  }

  private fun getIds(index: Long): ConcurrentHashSetLong {
    val set = cache[index]
    if (set != null) {
      return set
    }
    return cache.computeIfAbsent(index) { k ->
      val idSet = entityCacheLoader.loadIdsByIndex(entityClass, indexName, k)
        .collect({ ConcurrentHashSetLong() }, { set, id -> set.add(id) })
        .block(Duration.ofSeconds(5))!!
      getAllCached()
        .filter { indexMapper.applyAsLong(it) == k }
        .forEach { idSet.add(it.id) }
      idSet
    }
  }

  override fun getByIndex(index: Long): List<E> {
    return getAll(getIds(index))
  }

  override fun getIndexSize(index: Long): Int {
    return getIds(index).size
  }

  override fun create(e: E): E {
    val idSet = getIds(indexMapper.applyAsLong(e))
    val entity = entityCache.create(e)
    idSet.add(entity.id)
    return entity
  }

  override fun getOrCreate(id: Long, creation: (Long) -> E): E {
    return entityCache.getOrCreate(id) { k ->
      val entity = creation(k)
      getIds(indexMapper.applyAsLong(entity)).add(entity.id)
      entity
    }
  }

  override fun delete(e: E) {
    val idSet = cache[indexMapper.applyAsLong(e)]
    entityCache.delete(e)
    idSet?.remove(e.id)
  }

  override fun delete(id: Long) {
    val entity = getOrNull(id)
    entityCache.delete(id)
    val idSet = cache[indexMapper.applyAsLong(entity)]
    idSet?.remove(id)
  }
}
