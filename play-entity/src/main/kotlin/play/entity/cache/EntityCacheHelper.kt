package play.entity.cache

import mu.KLogging
import play.entity.Entity

/**
 *
 * @author LiangZengle
 */
object EntityCacheHelper : KLogging() {

  @JvmStatic
  fun getInitialSizeOrDefault(entityClass: Class<*>, default: Int): Int {
    return entityClass.getAnnotation(InitialCacheSize::class.java)?.value?.let {
      when (it.first()) {
        'x', 'X' -> it.substring(1).toInt() * default
        else -> it.toInt()
      }
    } ?: default
  }

  @JvmStatic
  fun reportMissingInitialCacheSize(entityClass: Class<*>) {
    val shouldSpecifyInitialCacheSize = entityClass.getAnnotation(ShouldSpecifyInitialCacheSize::class.java) ?: return
    if (!shouldSpecifyInitialCacheSize.value) {
      return
    }
    val initialCacheSize = entityClass.getAnnotation(InitialCacheSize::class.java)
    if (initialCacheSize == null) {
      logger.warn { "Probably missing @${InitialCacheSize::class.simpleName} on ${entityClass.name}. You can use @${ShouldSpecifyInitialCacheSize::class.simpleName}(false) to suppress waring." }
    }
  }
  
  @JvmStatic
  internal fun isNeverExpire(entityClass: Class<out Entity<*>>): Boolean {
    val cacheSpec = entityClass.getAnnotation(CacheSpec::class.java)
    return cacheSpec == null || cacheSpec.neverExpire || cacheSpec.expireEvaluator == NeverExpireEvaluator::class
  }
}
