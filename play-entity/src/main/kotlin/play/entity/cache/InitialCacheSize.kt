package play.entity.cache

import java.lang.annotation.Inherited

/**
 * 缓存初始大小
 *
 * @property value
 */
@Inherited
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class InitialCacheSize(val value: String) {
  companion object {
    /**
     * 1
     */
    const val ONE = "1"

    /**
     * 16
     */
    const val SMALL = "16"

    /**
     * play.entity.cache.initial-size
     */
    const val DEFAULT = "x1"
  }
}
