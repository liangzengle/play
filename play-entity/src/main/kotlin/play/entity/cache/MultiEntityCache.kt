package play.entity.cache

import com.github.benmanes.caffeine.cache.Caffeine
import play.entity.EntityHelper
import play.entity.ObjId
import play.entity.ObjIdEntity
import play.scheduling.Scheduler
import play.util.collection.ConcurrentHashSet
import play.util.reflect.Reflect
import java.time.Duration
import kotlin.time.Duration.Companion.seconds

interface MultiEntityCache<K, ID : ObjId, E : ObjIdEntity<ID>> : EntityCache<ID, E> {
  fun getMulti(key: K): Collection<E>
}

class MultiEntityCacheImpl<K, ID : ObjId, E : ObjIdEntity<ID>>(
  private val entityCache: EntityCache<ID, E>,
  private val entityCacheLoader: EntityCacheLoader,
  private val scheduler: Scheduler,
  private val expireEvaluator: MultiCacheExpireEvaluator<E, K>?
) : MultiEntityCache<K, ID, E>, EntityCache<ID, E> by entityCache {

  private val multiKeyAccessor = EntityCacheHelper.getMultiKeyAccessor<K>(
    Reflect.getRawClass<ID>(EntityHelper.getIdType(entityClass))
  )

  private val multiIdCache =
    Caffeine.newBuilder().expireAfterAccess(Duration.ofMinutes(10)).build<K, MutableSet<ID>> { key ->
      val keyName = multiKeyAccessor.getKeyName()
      val loadIds = entityCacheLoader.listMultiIds(entityClass, keyName, key).blockingGet(5.seconds)
      val idSet = getCachedEntities()
        .filter { it.multiKey() == key }
        .map { it.id }
        .toCollection(ConcurrentHashSet())
      idSet.addAll(loadIds)
      idSet
    }

  init {
    if (expireEvaluator != null) {
      scheduler.scheduleWithFixedDelay(Duration.ofMinutes(5), ::keepAlive)
    }
  }

  private fun keepAlive() {
    val expireEvaluator = this.expireEvaluator ?: return
    for (key in multiIdCache.asMap().keys) {
      if (!expireEvaluator.canExpire(key)) {
        multiIdCache.getIfPresent(key)
      }
    }
  }

  private fun getIds(multiKey: K): MutableSet<ID> {
    return multiIdCache.get(multiKey)!!
  }

  override fun getMulti(key: K): List<E> {
    val idSet = multiIdCache.get(key)!!
    return getAll(idSet)
  }

  override fun create(e: E): E {
    val idSet = getIds(e.multiKey())
    val entity = entityCache.create(e)
    idSet.add(entity.id)
    return entity
  }

  override fun getOrCreate(id: ID, creation: (ID) -> E): E {
    return entityCache.getOrCreate(id) { k ->
      val entity = creation(k)
      getIds(entity.multiKey()).add(entity.id)
      entity
    }
  }

  override fun delete(e: E) {
    val idSet = getIds(e.multiKey())
    entityCache.delete(e)
    idSet.remove(e.id)
  }

  override fun delete(id: ID) {
    val entity = getOrNull(id) ?: return
    val idSet = getIds(entity.multiKey())
    entityCache.delete(id)
    idSet.remove(id)
  }

  @Suppress("NOTHING_TO_INLINE")
  private inline fun E.multiKey(): K = multiKeyAccessor.getValue(this)
}
