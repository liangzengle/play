package play.inject

import kotlin.reflect.KClass

/**
 * Created by LiangZengle on 2020/2/16.
 */
typealias PlayInjector = Injector

interface Injector {

  fun <T> getInstance(type: Class<T>): T

  fun <T> getInstance(type: Class<T>, name: String): T

  fun <T : Any> getInstance(type: KClass<T>): T = getInstance(type.java)

  fun <T> getInstancesOfType(type: Class<T>): List<T>

  fun <T : Any> getInstancesOfType(type: KClass<T>): List<T> = getInstancesOfType(type.java)
}

inline fun <reified T> Injector.getInstance(): T = getInstance(T::class.java)

inline fun <reified T> Injector.getInstance(name: String): T = getInstance(T::class.java, name)

