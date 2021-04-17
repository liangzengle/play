package play.scheduling.inject

import com.google.auto.service.AutoService
import com.google.inject.Module
import play.inject.guice.GuiceModule
import play.scheduling.DefaultScheduler
import play.scheduling.Scheduler
import play.util.concurrent.NamedThreadFactory
import java.util.concurrent.Executor
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledThreadPoolExecutor
import javax.inject.Inject
import javax.inject.Provider

/**
 * Scheduling Module
 *
 * @author LiangZengle
 */
@AutoService(Module::class)
class SchedulingGuiceModule : GuiceModule() {
  override fun configure() {
    bindDefaultProvider<ScheduledExecutorService, ScheduledExecutorServiceProvider>()
    bindDefaultProvider<Scheduler, DefaultSchedulerProvider>()
  }
}

internal class ScheduledExecutorServiceProvider : Provider<ScheduledExecutorService> {
  private val executor by lazy {
    val threadFactory = NamedThreadFactory.newBuilder("scheduler").daemon(true).build()
    val executor = ScheduledThreadPoolExecutor(1, threadFactory)
    executor.prestartCoreThread()
    executor.removeOnCancelPolicy = true
    executor
  }

  override fun get(): ScheduledExecutorService = executor
}

internal class DefaultSchedulerProvider @Inject constructor(
  scheduledExecutorServiceProvider: ScheduledExecutorServiceProvider,
  executor: Executor
) : Provider<Scheduler> {

  private val value by lazy {
    DefaultScheduler(scheduledExecutorServiceProvider.get(), executor)
  }

  override fun get(): Scheduler = value

}
