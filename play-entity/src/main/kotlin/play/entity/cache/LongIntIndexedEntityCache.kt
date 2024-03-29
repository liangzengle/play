package play.entity.cache

import org.eclipse.collections.impl.factory.primitive.IntLists
import play.entity.LongIdEntity
import play.scheduling.Scheduler
import play.util.collection.ConcurrentHashSetLong
import play.util.collection.ConcurrentIntObjectMap
import play.util.getOrNull
import play.util.max
import play.util.min
import java.time.Duration
import java.util.function.ToIntFunction

/**
 * Long id, Int index
 */
class LongIntIndexedEntityCache<E : LongIdEntity>(
  private val indexMapper: ToIntFunction<E>,
  private val entityCache: EntityCacheLong<E>,
  private val entityCacheLoader: EntityCacheLoader,
  scheduler: Scheduler,
  keepAlive: Duration
) : IndexedEntityCache<Int, Long, E>, EntityCacheLong<E> by entityCache, EntityCacheInternalApi<E> {

  private val cache = ConcurrentIntObjectMap<ConcurrentHashSetLong>()

  init {
    require(entityCache is EntityCacheInternalApi<*>) { "${entityCache.javaClass} must implement EntityCacheInternalApi" }
    if (!EntityCacheHelper.isNeverExpire(entityClass)) {
      val interval = Duration.ZERO max (keepAlive.dividedBy(2) min keepAlive)
      require(interval > Duration.ZERO) { "interval must be positive" }
      scheduler.scheduleWithFixedDelay(interval, ::evict)
    }
  }

  private fun evict() {
    fun canExpire(index: Int): Boolean {
      val expireEvaluator = expireEvaluator()
      for (id in getIds(index)) {
        val entity = entityCache.getCached(id).getOrNull() ?: continue
        if (!expireEvaluator.canExpire(entity)) {
          return false
        }
      }
      return true
    }

    val toBeEvict = IntLists.mutable.empty()
    for (entry in cache) {
      val index = entry.key
      val value = entry.value
      if (value.isEmpty() || canExpire(index)) {
        toBeEvict.add(index)
      }
    }
    for (i in 0 ..< toBeEvict.size()) {
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

  private fun getIds(index: Int): ConcurrentHashSetLong {
    val set = cache[index]
    if (set != null) {
      return set
    }
    return cache.computeIfAbsent(index) { k ->
      val idSet = entityCacheLoader.loadIdsByCacheIndex(entityClass, k)
        .collect({ ConcurrentHashSetLong() }, { set, id -> set.add(id) })
        .block(Duration.ofSeconds(5))!!
      getAllCached()
        .filter { indexMapper.applyAsInt(it) == k }
        .forEach { idSet.add(it.id) }
      idSet
    }
  }

  override fun getByIndex(index: Int): MutableList<E> {
    return getAll(getIds(index))
  }

  override fun getIndexSize(index: Int): Int {
    return getIds(index).size
  }

  override fun create(e: E): E {
    val idSet = getIds(indexMapper.applyAsInt(e))
    val entity = entityCache.create(e)
    idSet.add(entity.id)
    return entity
  }

  override fun getOrCreate(id: Long, creation: (Long) -> E): E {
    return entityCache.getOrCreate(id) { k ->
      val entity = creation(k)
      getIds(indexMapper.applyAsInt(entity)).add(entity.id)
      entity
    }
  }

  override fun delete(e: E) {
    val idSet = cache[indexMapper.applyAsInt(e)]
    entityCache.delete(e)
    idSet?.remove(e.id)
  }

  override fun delete(id: Long) {
    val entity = getOrNull(id)
    entityCache.delete(id)
    val idSet = cache[indexMapper.applyAsInt(entity)]
    idSet?.remove(id)
  }
}
