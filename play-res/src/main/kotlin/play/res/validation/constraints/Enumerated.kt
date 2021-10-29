package play.res.validation.constraints

import play.util.enumration.IdEnum
import kotlin.reflect.KClass

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class Enumerated<T>(val value: KClass<out Enum<*>>) where T : Enum<*>, T : IdEnum<*>
