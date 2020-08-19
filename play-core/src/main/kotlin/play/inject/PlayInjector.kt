package play.inject

import kotlin.reflect.KClass

/**
 * Created by LiangZengle on 2020/2/16.
 */
interface PlayInjector {

  fun <T> getInstance(type: Class<T>): T

  fun <T> getInstance(type: Class<T>, name: String): T

  fun <T : Any> getInstance(type: KClass<T>): T = getInstance(type.java)

  fun <T> getInstancesOfType(type: Class<T>): Collection<T>

  fun <T : Any> getInstancesOfType(type: KClass<T>): Collection<T> = getInstancesOfType(type.java)

  fun getInstancesAnnotatedWith(type: Class<out Annotation>): Collection<Any>
}

