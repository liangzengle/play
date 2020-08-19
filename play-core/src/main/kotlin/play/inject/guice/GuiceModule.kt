package play.inject.guice

import com.google.auto.service.AutoService
import com.google.inject.AbstractModule
import com.google.inject.Key
import com.google.inject.Module
import com.google.inject.TypeLiteral
import com.google.inject.binder.AnnotatedBindingBuilder
import com.google.inject.binder.LinkedBindingBuilder
import com.google.inject.binder.ScopedBindingBuilder
import com.google.inject.matcher.Matchers
import com.google.inject.multibindings.OptionalBinder
import com.google.inject.name.Names
import org.reflections.Reflections
import play.*
import play.inject.PlayInjector
import play.util.scheduling.executor.ScheduledExecutor
import java.util.concurrent.ScheduledExecutorService

/**
 * Created by LiangZengle on 2020/2/20.
 */
abstract class GuiceModule : AbstractModule() {
  protected lateinit var ctx: ApplicationLoader.Context

  fun initContext(ctx: ApplicationLoader.Context) {
    this.ctx = ctx
  }

  protected inline fun <reified T> bind(): AnnotatedBindingBuilder<T> = bind(T::class.java)

  protected inline fun <reified T> typeLiteral(): TypeLiteral<T> = object : TypeLiteral<T>() {}

  protected inline fun <reified T> AnnotatedBindingBuilder<T>.to(): ScopedBindingBuilder = this.to(typeLiteral())

  protected fun <T> AnnotatedBindingBuilder<T>.qualifiedWith(name: String): LinkedBindingBuilder<T> =
    this.annotatedWith(Names.named(name))

  protected fun <T, A : Annotation> AnnotatedBindingBuilder<T>.qualifiedWith(a: A): LinkedBindingBuilder<T> =
    this.annotatedWith(a)

  protected inline fun <reified T> LinkedBindingBuilder<in T>.to(): ScopedBindingBuilder = this.to(typeLiteral<T>())

  protected fun <T> AnnotatedBindingBuilder<T>.toBinding(key: Key<T>): ScopedBindingBuilder = this.to(key)

  protected inline fun <reified T> binding(): Key<T> = Key.get(typeLiteral<T>())

  protected inline fun <reified T> binding(name: String): Key<T> = Key.get(T::class.java, Names.named(name))

  protected inline fun <reified T> optionalBind(): OptionalBinder<T> =
    OptionalBinder.newOptionalBinder(binder(), T::class.java)

  protected inline fun <reified T> OptionalBinder<in T>.defaultBinding(key: Key<T>): ScopedBindingBuilder =
    this.setDefault().to(key)

  protected inline fun <reified T> OptionalBinder<in T>.defaultTo(): ScopedBindingBuilder =
    this.setDefault().to(typeLiteral<T>())

  protected fun ScopedBindingBuilder.eagerly() = this.asEagerSingleton()
}

@AutoService(Module::class)
internal class BootstrapGuiceModule : GuiceModule() {
  override fun configure() {
    binder().disableCircularProxies()
    bindListener(Matchers.any(), LifecycleBeanManager(ctx.lifecycle))

    bind<Configuration>().toInstance(ctx.conf)
    bind<Application>().to<DefaultApplication>()
    bind<Mode>().toInstance(ctx.mode)
    bind<ClassScanner>().toInstance(ctx.classScanner)
    bind<Reflections>().toInstance(ctx.classScanner.reflections)
    bind<ApplicationLifecycle>().toInstance(ctx.lifecycle)
    bind<GuiceInjector>().toProvider(GuiceInjectorProvider::class.java)
    bind<PlayInjector>().to<GuiceInjector>()
    bind<ScheduledExecutorService>().toInstance(ScheduledExecutor.get())
  }
}
