package play.entity.cache

/**
 *
 * @param fieldName field name in database, required when database field name is different from java field name
 *
 * @author LiangZengle
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class CacheIndex(val fieldName: String = "")
