package play.entity.cache


import play.entity.Entity
import play.inject.PlayInjector
import play.util.logging.WithLogger

/**
 *
 * @author LiangZengle
 */
object EntityCacheHelper : WithLogger() {

  private val DefaultCacheSpec = CacheSpec()

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
  fun isNeverExpire(entityClass: Class<out Entity<*>>): Boolean {
    val cacheSpec = getCacheSpec(entityClass)
    return cacheSpec.neverExpire || cacheSpec.expireEvaluator == NeverExpireEvaluator::class
  }

  @JvmStatic
  fun getCacheSpec(entityClass: Class<out Entity<*>>): CacheSpec {
    return entityClass.getAnnotation(CacheSpec::class.java) ?: DefaultCacheSpec
  }

  @JvmStatic
  fun isResident(entityClass: Class<out Entity<*>>): Boolean {
    val cacheSpec = getCacheSpec(entityClass)
    return cacheSpec.loadAllOnInit && isNeverExpire(entityClass)
  }

  @JvmStatic
  fun getExpireEvaluator(entityClass: Class<out Entity<*>>, injector: PlayInjector): ExpireEvaluator {
    val cacheSpec = getCacheSpec(entityClass)
    return if (cacheSpec.neverExpire) NeverExpireEvaluator else {
      when (val expireEvaluator = cacheSpec.expireEvaluator) {
        DefaultExpireEvaluator::class -> DefaultExpireEvaluator
        NeverExpireEvaluator::class -> NeverExpireEvaluator
        else -> injector.getInstance(expireEvaluator.java)
      }
    }
  }
}
