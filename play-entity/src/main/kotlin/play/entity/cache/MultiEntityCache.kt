package play.entity.cache

import org.eclipse.collections.impl.factory.primitive.IntLists
import org.eclipse.collections.impl.factory.primitive.LongLists
import play.entity.ObjId
import play.entity.ObjIdEntity
import play.scheduling.Scheduler
import play.util.collection.ConcurrentHashSet
import play.util.collection.ConcurrentIntObjectMap
import play.util.collection.ConcurrentLongObjectMap
import play.util.getOrNull
import play.util.max
import play.util.min
import play.util.unsafeCast
import java.time.Duration
import java.util.function.ToIntFunction
import java.util.function.ToLongFunction
import kotlin.time.Duration.Companion.seconds

interface MultiEntityCache<K, ID : ObjId, E : ObjIdEntity<ID>> : EntityCache<ID, E> {
  fun getMulti(key: K): List<E>
}

class MultiEntityCacheLong<ID : ObjId, E : ObjIdEntity<ID>>(
  private val keyName: String,
  private val keyMapper: ToLongFunction<E>,
  private val entityCache: EntityCache<ID, E>,
  private val entityCacheLoader: EntityCacheLoader,
  scheduler: Scheduler,
  keepAlive: Duration
) : MultiEntityCache<Long, ID, E>, EntityCache<ID, E> by entityCache {

  private val cache = ConcurrentLongObjectMap<MutableSet<ID>>()

  init {
    require(EntityCacheInternalApi::class.java.isInstance(entityCache)) { "${entityCache.javaClass} must implement EntityCacheInternalApi" }
    if (EntityCacheHelper.isNeverExpire(entityClass)) {
      val interval = Duration.ZERO max (keepAlive.dividedBy(2) min keepAlive)
      require(interval > Duration.ZERO) { "interval must be positive" }
      scheduler.scheduleWithFixedDelay(interval, ::evict)
    }
  }

  private fun internalApi(): EntityCacheInternalApi<E> {
    return entityCache.unsafeCast()
  }

  private fun evict() {
    fun canExpire(key: Long): Boolean {
      val expireEvaluator = internalApi().expireEvaluator()
      for (id in getIds(key)) {
        val entity = entityCache.getCached(id).getOrNull() ?: continue
        if (!expireEvaluator.canExpire(entity)) {
          return false
        }
      }
      return true
    }

    val toBeEvict = LongLists.mutable.empty()
    for (entry in cache) {
      val key = entry.key
      val value = entry.value
      if (value.isEmpty() || canExpire(key)) {
        toBeEvict.add(entry.key)
      }
    }
    for (i in 0 until toBeEvict.size()) {
      val key = toBeEvict.get(i)
      cache.computeIfPresent(key) { k, v ->
        if (v.isEmpty() || canExpire(k)) {
          null
        } else {
          v
        }
      }
    }
  }

  private fun getIds(key: Long): MutableSet<ID> {
    val set = cache[key]
    if (set != null) {
      return set
    }
    return cache.computeIfAbsent(key) { k ->
      val loadIds = entityCacheLoader.listMultiIds(entityClass, keyName, k).blockingGet(10.seconds)
      val idSet = internalApi()
        .getAllCached()
        .filter { keyMapper.applyAsLong(it) == k }
        .map { it.id }
        .toCollection(ConcurrentHashSet())
      idSet.addAll(loadIds)
      idSet
    }
  }

  override fun getMulti(key: Long): List<E> {
    val idSet = getIds(key)
    return getAll(idSet)
  }

  override fun create(e: E): E {
    val idSet = getIds(keyMapper.applyAsLong(e))
    val entity = entityCache.create(e)
    idSet.add(entity.id)
    return entity
  }

  override fun getOrCreate(id: ID, creation: (ID) -> E): E {
    return entityCache.getOrCreate(id) { k ->
      val entity = creation(k)
      getIds(keyMapper.applyAsLong(entity)).add(entity.id)
      entity
    }
  }

  override fun delete(e: E) {
    val idSet = getIds(keyMapper.applyAsLong(e))
    entityCache.delete(e)
    idSet.remove(e.id)
  }

  override fun delete(id: ID) {
    val entity = getOrNull(id) ?: return
    val idSet = getIds(keyMapper.applyAsLong(entity))
    entityCache.delete(id)
    idSet.remove(id)
  }
}

class MultiEntityCacheInt<ID : ObjId, E : ObjIdEntity<ID>>(
  private val keyName: String,
  private val keyMapper: ToIntFunction<E>,
  private val entityCache: EntityCache<ID, E>,
  private val entityCacheLoader: EntityCacheLoader,
  scheduler: Scheduler,
  keepAlive: Duration
) : MultiEntityCache<Int, ID, E>, EntityCache<ID, E> by entityCache {

  private val cache = ConcurrentIntObjectMap<MutableSet<ID>>()

  init {
    require(EntityCacheInternalApi::class.java.isInstance(entityCache)) { "${entityCache.javaClass} must implement EntityCacheInternalApi" }
    if (!EntityCacheHelper.isNeverExpire(entityClass)) {
      val interval = Duration.ZERO max (keepAlive.dividedBy(2) min keepAlive)
      require(interval > Duration.ZERO) { "interval must be positive" }
      scheduler.scheduleWithFixedDelay(interval, ::evict)
    }
  }

  private fun internalApi(): EntityCacheInternalApi<E> {
    return entityCache.unsafeCast()
  }

  private fun evict() {
    fun canExpire(key: Int): Boolean {
      val expireEvaluator = internalApi().expireEvaluator()
      for (id in getIds(key)) {
        val entity = entityCache.getCached(id).getOrNull() ?: continue
        if (!expireEvaluator.canExpire(entity)) {
          return false
        }
      }
      return true
    }

    val toBeEvict = IntLists.mutable.empty()
    for (entry in cache) {
      val key = entry.key
      val value = entry.value
      if (value.isEmpty() || canExpire(key)) {
        toBeEvict.add(entry.key)
      }
    }
    for (i in 0 until toBeEvict.size()) {
      val key = toBeEvict.get(i)
      cache.computeIfPresent(key) { k, v ->
        if (v.isEmpty() || canExpire(k)) {
          null
        } else {
          v
        }
      }
    }
  }

  private fun getIds(key: Int): MutableSet<ID> {
    val set = cache[key]
    if (set != null) {
      return set
    }
    return cache.computeIfAbsent(key) { k ->
      val loadIds = entityCacheLoader.listMultiIds(entityClass, keyName, k).blockingGet(10.seconds)
      val idSet = internalApi()
        .getAllCached()
        .filter { keyMapper.applyAsInt(it) == k }
        .map { it.id }
        .toCollection(ConcurrentHashSet())
      idSet.addAll(loadIds)
      idSet
    }
  }

  override fun getMulti(key: Int): List<E> {
    val idSet = getIds(key)
    return getAll(idSet)
  }

  override fun create(e: E): E {
    val idSet = getIds(keyMapper.applyAsInt(e))
    val entity = entityCache.create(e)
    idSet.add(entity.id)
    return entity
  }

  override fun getOrCreate(id: ID, creation: (ID) -> E): E {
    return entityCache.getOrCreate(id) { k ->
      val entity = creation(k)
      getIds(keyMapper.applyAsInt(entity)).add(entity.id)
      entity
    }
  }

  override fun delete(e: E) {
    val idSet = getIds(keyMapper.applyAsInt(e))
    entityCache.delete(e)
    idSet.remove(e.id)
  }

  override fun delete(id: ID) {
    val entity = getOrNull(id) ?: return
    val idSet = getIds(keyMapper.applyAsInt(entity))
    entityCache.delete(id)
    idSet.remove(id)
  }
}
