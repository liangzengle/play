package play.inject

import com.google.inject.Injector
import com.google.inject.Key
import com.google.inject.name.Names
import javax.inject.Singleton
import kotlin.streams.toList
import play.util.reflect.ClassScanner

internal class PlayInjectImpl(private val guice: Injector, private val classScanner: ClassScanner) :
  PlayInjector, Injector by guice {

  override fun <T> getInstance(type: Class<T>): T {
    return guice.getInstance(type)
  }

  override fun <T> getInstance(type: Class<T>, name: String): T {
    return guice.getInstance(Key.get(type, Names.named(name)))
  }

  override fun <T> getInstancesOfType(type: Class<T>): List<T> {
    return classScanner.getOrdinarySubTypesStream(type)
      .filter {
        it.isAnnotationPresent(Singleton::class.java) || it.isAnnotationPresent(com.google.inject.Singleton::class.java)
      }
      .map { guice.getInstance(it) }
      .toList()
  }
}
