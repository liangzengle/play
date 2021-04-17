package play.codegen

import kotlin.reflect.KClass

/**
 * 枚举类型
 * @author LiangZengle
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class EnumType(val value: KClass<*>)
