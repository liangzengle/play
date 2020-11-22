package play.inject.guice

import com.google.inject.Injector
import com.google.inject.Key
import com.google.inject.name.Names
import play.ClassScanner
import play.inject.PlayInjector
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import kotlin.streams.toList

class GuiceInjector(private val guice: Injector, private val classScanner: ClassScanner) :
  PlayInjector, Injector by guice {

  override fun <T> getInstance(type: Class<T>): T {
    return guice.getInstance(type)
  }

  override fun <T> getInstance(type: Class<T>, name: String): T {
    return guice.getInstance(Key.get(type, Names.named(name)))
  }

  override fun <T> getInstancesOfType(type: Class<T>): List<T> {
    return classScanner.getConcreteSubTypesStream(type)
      .filter {
        it.isAnnotationPresent(Singleton::class.java) || it.isAnnotationPresent(com.google.inject.Singleton::class.java)
      }
      .map { guice.getInstance(it) }
      .toList()
  }

  override fun <T> getInstanceOrNull(type: Class<T>): T? {
    return guice.getExistingBinding(Key.get(type))?.provider?.get()
  }

  override fun <T> getInstanceOrNull(type: Class<T>, name: String): T? {
    return guice.getExistingBinding(Key.get(type, Names.named(name)))?.provider?.get()
  }
}

@Singleton
class GuiceInjectorProvider @Inject constructor(injector: Injector, classScanner: ClassScanner) :
  Provider<GuiceInjector> {
  private val guiceInjector = GuiceInjector(injector, classScanner)
  override fun get(): GuiceInjector {
    return guiceInjector
  }
}
