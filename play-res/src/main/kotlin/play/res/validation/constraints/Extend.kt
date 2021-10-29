package play.res.validation.constraints

import play.res.AbstractResource
import kotlin.reflect.KClass

/**
 * 表示id与[table]表的id完全一致
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class Extend(val table: KClass<out AbstractResource>)
