package play.inject.guice

import javax.inject.Inject
import javax.inject.Provider
import play.inject.Injector

@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS)
annotation class EnableMultiBinding

abstract class GeneratedMultiBindModule : GuiceModule()

class MultiBindListProvider<T>(private val type: Class<T>) : Provider<List<T>> {
  @Inject
  private lateinit var injector: Injector

  override fun get(): List<T> {
    return injector.getInstancesOfType(type)
  }
}

class MultiBindSetProvider<T>(private val type: Class<T>) : Provider<Set<T>> {
  @Inject
  private lateinit var injector: Injector

  override fun get(): Set<T> {
    return injector.getInstancesOfType(type).toSet()
  }
}
