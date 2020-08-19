package play.inject

import kotlin.reflect.KClass

/**
 * Created by LiangZengle on 2020/2/16.
 */
typealias PlayInjector = Injector

interface Injector {

  fun <T> instanceOf(type: Class<T>): T

  fun <T> instanceOf(type: Class<T>, name: String): T

  fun <T : Any> instanceOf(type: KClass<T>): T = instanceOf(type.java)

  fun <T> instancesOf(type: Class<T>): List<T>

  fun <T : Any> instancesOf(type: KClass<T>): List<T> = instancesOf(type.java)
}

inline fun <reified T> Injector.instanceOf(): T = instanceOf(T::class.java)

inline fun <reified T> Injector.instanceOf(name: String): T = instanceOf(T::class.java, name)
