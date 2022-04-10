package play.entity.cache

/**
 *
 * @author LiangZengle
 */
interface EntityCacheInternalApi {

  fun expireEvaluator(): ExpireEvaluator
}
