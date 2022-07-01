package play.entity.cache

import play.entity.Entity

/**
 *
 * @author LiangZengle
 */
interface EntityCacheInternalApi<E : Entity<*>> {

  fun expireEvaluator(): ExpireEvaluator

  /**
   * 获取当前缓存中的所有实体
   *
   * @return 缓存中所有实体
   */
  fun getAllCached(): Sequence<E>
}
