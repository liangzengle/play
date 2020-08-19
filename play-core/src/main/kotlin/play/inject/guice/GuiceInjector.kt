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

class GuiceInjector(private val guice: Injector, private val classScanner: ClassScanner) : PlayInjector,
  Injector by guice {
  override fun <T> getInstance(type: Class<T>): T {
    return guice.getInstance(type)
  }

  fun <T> getInstance(type: Class<T>, name: String): T {
    return guice.getInstance(Key.get(type, Names.named(name)))
  }

  override fun <E> instanceOf(type: Class<E>): E {
    return getInstance(type)
  }

  override fun <E> instanceOf(type: Class<E>, name: String): E {
    return guice.getInstance(Key.get(type, Names.named(name)))
  }

  override fun <E> instancesOf(type: Class<E>): List<E> {
    return classScanner.getSubTypesStream(type)
      .filter {
        it.isAnnotationPresent(Singleton::class.java) || it.isAnnotationPresent(com.google.inject.Singleton::class.java)
      }
      .map { guice.getInstance(it) }
      .toList()
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
