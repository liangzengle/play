package play.entity.cache

import java.lang.annotation.Inherited

/**
 * 当没有指定初始缓存大小时，给出警告
 *
 * @property value 是否警告
 */
@Inherited
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class ShouldSpecifyInitialCacheSize(val value: Boolean = true)
