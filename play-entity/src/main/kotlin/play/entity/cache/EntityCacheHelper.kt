package play.entity.cache

/**
 *
 * @author LiangZengle
 */
object EntityCacheHelper {

  fun getInitialSizeOrDefault(entityClass: Class<*>, default: Int): Int {
    return entityClass.getAnnotation(CacheSpec::class.java)?.initialSize?.let {
      when (it.first()) {
        'x', 'X' -> it.substring(1).toInt() * default
        else -> it.toInt()
      }
    } ?: default
  }
}
