package play.inject.guice

import com.google.inject.AbstractModule
import com.google.inject.Key
import com.google.inject.Scopes
import com.google.inject.TypeLiteral
import com.google.inject.binder.AnnotatedBindingBuilder
import com.google.inject.binder.LinkedBindingBuilder
import com.google.inject.binder.ScopedBindingBuilder
import com.google.inject.multibindings.OptionalBinder
import com.google.inject.name.Names
import javax.inject.Provider
import play.inject.NullProvider

/**
 *
 * @author LiangZengle
 */
abstract class GuiceModule : AbstractModule() {

  abstract override fun configure()

  protected inline fun <reified T, reified T1 : T> bindDefault() {
    OptionalBinder.newOptionalBinder(binder(), T::class.java).setDefault().to(T1::class.java).asSingleton()
  }

  protected inline fun <reified T, reified P : Provider<T>> bindDefaultProvider() {
    bind(P::class.java).asSingleton()
    OptionalBinder.newOptionalBinder(binder(), T::class.java).setDefault().toProvider(P::class.java).asSingleton()
  }

  protected inline fun <reified T> bindDefaultInstance(default: T) {
    OptionalBinder.newOptionalBinder(binder(), T::class.java).setDefault().toInstance(default)
  }

  protected fun <T> bindDefaultInstance(binding: Key<T>, default: T) {
    OptionalBinder.newOptionalBinder(binder(), binding).setDefault().toInstance(default)
  }

  protected inline fun <reified T> bindDefaultToNull() {
    OptionalBinder.newOptionalBinder(binder(), T::class.java).setDefault().toProvider(NullProvider.of())
  }

  protected inline fun <reified T, reified P : Provider<T>> bindProvider() {
    bind(P::class.java).asSingleton()
    bind(T::class.java).toProvider(P::class.java).asSingleton()
  }

  protected inline fun <reified T> bindSingleton() = bind(T::class.java).asSingleton()

  protected inline fun <reified T> bindEagerlySingleton() = bind(T::class.java).asEagerSingleton()

  protected inline fun <reified T> bind(): AnnotatedBindingBuilder<T> = bind(T::class.java)

  protected inline fun <reified T> bindNamed(name: String): LinkedBindingBuilder<T> =
    bind(T::class.java).annotatedWith(Names.named(name))

  protected inline fun <reified T, reified P : Provider<T>> bindNamedToProvider(name: String) {
    bind(P::class.java).asSingleton()
    bind(T::class.java).annotatedWith(Names.named(name)).toProvider(P::class.java).asSingleton()
  }

  protected inline fun <reified T> binding(name: String): Key<T> = Key.get(T::class.java, Names.named(name))

  protected inline fun <reified T> binding(): Key<T> = Key.get(T::class.java)

  protected inline fun <reified T> typeLiteral(): TypeLiteral<T> = object:TypeLiteral<T>(){}

  protected inline fun <reified T> bindToBinding(binding: Key<out T>) = bind(T::class.java).to(binding).asSingleton()

  protected inline fun <reified T, reified T1 : T> overrideDefaultBinding() {
    OptionalBinder.newOptionalBinder(binder(), T::class.java).setBinding().to(T1::class.java).asSingleton()
  }

  protected inline fun <reified T> overrideDefaultBindingWith(by: Key<out T>) {
    OptionalBinder.newOptionalBinder(binder(), T::class.java).setBinding().to(by).asSingleton()
  }

  protected inline fun <reified T, reified P : Provider<T>> overrideDefaultBindingByProvider() {
    bind(P::class.java).asSingleton()
    OptionalBinder.newOptionalBinder(binder(), T::class.java).setBinding().toProvider(P::class.java).asSingleton()
  }

  protected inline fun <reified T : Any> overrideDefaultBindingByInstance(value: T) {
    OptionalBinder.newOptionalBinder(binder(), T::class.java).setBinding().toInstance(value)
  }

  protected fun ScopedBindingBuilder.asSingleton() = `in`(Scopes.SINGLETON)

  protected fun ScopedBindingBuilder.asPrototype() = `in`(Scopes.NO_SCOPE)
}
