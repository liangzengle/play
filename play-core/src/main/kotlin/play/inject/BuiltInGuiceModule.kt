package play.inject

import com.google.auto.service.AutoService
import com.google.inject.Injector
import com.google.inject.Module
import com.google.inject.Provides
import com.google.inject.matcher.Matchers
import com.typesafe.config.Config
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import javax.inject.Singleton
import play.*
import play.ApplicationImpl
import play.inject.guice.ContextualGuiceModule
import play.util.concurrent.CommonPool
import play.util.reflect.ClassScanner

/**
 *
 * @author LiangZengle
 */
@AutoService(Module::class)
class BuiltInGuiceModule : ContextualGuiceModule() {
  override fun configure() {
    bind<Config>().toInstance(conf)
    bind<ShutdownCoordinator>().toInstance(shutdownCoordinator)

    bind<ExecutorService>().toInstance(CommonPool)
    bindToBinding<Executor>(binding<ExecutorService>())

    val jarsToScan = conf.getStringList("reflection.jars-to-scan")
    val packagesToScan = conf.getStringList("reflection.packages-to-scan")
    val classScanner = ClassScanner(CommonPool, jarsToScan, packagesToScan)
    bind<ClassScanner>().toInstance(classScanner)

    val eventBus = ApplicationEventBus()
    bind<ApplicationEventBus>().toInstance(eventBus)
    bindListener(Matchers.any(), EventReceiverProvisionListener(eventBus))

    bindSingleton<ApplicationImpl>()
    bindToBinding<Application>(binding<ApplicationImpl>())
  }

  @Provides
  @Singleton
  fun playInjector(inject: Injector, classScanner: ClassScanner): PlayInjector {
    return PlayInjectImpl(inject, classScanner)
  }
}
