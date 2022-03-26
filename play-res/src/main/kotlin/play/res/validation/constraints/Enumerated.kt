package play.res.validation.constraints

import play.util.enumeration.IdEnum
import kotlin.reflect.KClass

/**
 * 表示配置表id跟枚举类一一对应
 *
 * @property value 枚举类型
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class Enumerated<T>(val value: KClass<out Enum<*>>) where T : Enum<*>, T : IdEnum
