package play

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import org.springframework.beans.factory.ObjectFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.springframework.scheduling.TaskScheduler
import play.ShutdownCoordinator
import play.scheduling.DefaultScheduler
import play.scheduling.Scheduler
import play.scheduling.SpringTaskScheduler
import play.util.concurrent.CommonPool
import play.util.concurrent.NamedThreadFactory
import play.util.reflect.ClassScanner
import java.time.Clock
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledThreadPoolExecutor

/**
 *
 * @author LiangZengle
 */
@Configuration(proxyBeanMethods = false)
class PlayCoreConfiguration {

  @Bean
  @ConditionalOnMissingBean(Clock::class)
  fun clock(): Clock {
    return Clock.systemDefaultZone()
  }

  @Bean
  @ConditionalOnMissingBean
  fun config(@Autowired(required = false) @Qualifier("mainConfig") mainConfig: Config?): Config {
    val referenceConf = ConfigFactory.parseResources("reference.conf")
    val applicationConf = ConfigFactory.defaultApplication()
    var mainConf = mainConfig?.withFallback(applicationConf) ?: applicationConf
    mainConf = mainConf.withFallback(referenceConf).resolve()
    return mainConf
  }

  @Bean
  @ConditionalOnMissingBean(ShutdownCoordinator::class)
  fun shutdownCoordinator(): ShutdownCoordinator {
    return DefaultShutdownCoordinator()
  }

  @Bean
  @ConditionalOnMissingBean(ClassScanner::class)
  fun classScanner(conf: Config): ClassScanner {
    val jarsToScan = conf.getStringList("play.reflection.jars-to-scan")
    val packagesToScan = conf.getStringList("play.reflection.packages-to-scan")
    return ClassScanner(CommonPool, jarsToScan, packagesToScan)
  }

  @Bean
  @ConditionalOnMissingBean(ExecutorService::class)
  fun executorService(): ExecutorService {
    return CommonPool
  }

  @Bean
  @ConditionalOnMissingBean(Executor::class)
  fun executor(): Executor {
    return CommonPool
  }

  @Bean
  @ConditionalOnMissingBean(value = [Scheduler::class, ScheduledExecutorService::class])
  @Lazy
  fun scheduledExecutorService(): ScheduledExecutorService {
    val threadFactory = NamedThreadFactory.newBuilder("scheduler").daemon(true).build()
    val executor = ScheduledThreadPoolExecutor(1, threadFactory)
    executor.prestartCoreThread()
    executor.removeOnCancelPolicy = true
    return executor
  }

  @Bean
  @ConditionalOnMissingBean(Scheduler::class)
  fun scheduler(scheduleService: ObjectFactory<ScheduledExecutorService>, executor: Executor, clock: Clock): Scheduler {
    return DefaultScheduler(scheduleService.`object`, executor, clock)
  }

  @Bean
  fun taskScheduler(scheduler: Scheduler, clock: Clock): TaskScheduler {
    return SpringTaskScheduler(scheduler, clock)
  }
}
