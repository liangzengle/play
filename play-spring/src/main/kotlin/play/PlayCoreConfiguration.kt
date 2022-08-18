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
import play.scheduling.DefaultScheduler
import play.scheduling.Scheduler
import play.util.concurrent.CommonPool
import play.util.concurrent.threadFactory
import play.util.reflect.ClassScanner
import play.util.reflect.ClassgraphClassScanner
import play.util.time.Time
import java.time.Clock
import java.util.concurrent.*


/**
 *
 * @author LiangZengle
 */
@Configuration(proxyBeanMethods = false)
class PlayCoreConfiguration {

  @Bean
  @ConditionalOnMissingBean(Clock::class)
  fun clock(): Clock {
    return Time.clock()
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
  @ConditionalOnMissingBean
  fun classScanner(conf: Config): ClassScanner {
    val jarsToScan = conf.getStringList("play.reflection.jars-to-scan")
    val packagesToScan = conf.getStringList("play.reflection.packages-to-scan")
    return ClassgraphClassScanner(CommonPool, jarsToScan, packagesToScan)
  }

  @Bean
  @ConditionalOnMissingBean
  fun executorService(): ExecutorService {
    return CommonPool
  }

  @Bean
  @ConditionalOnMissingBean
  fun executor(): Executor {
    return CommonPool
  }

  @Bean
  @ConditionalOnMissingBean(value = [Scheduler::class, ScheduledExecutorService::class])
  @Lazy
  fun scheduledExecutorService(): ScheduledExecutorService {
    val threadFactory = threadFactory("scheduler", true)
    val executor = object : ScheduledThreadPoolExecutor(1, threadFactory) {
      override fun afterExecute(r: Runnable?, t: Throwable?) {
        super.afterExecute(r, t)
        var ex = t
        if (ex == null && r is Future<*> && r.isDone) {
          try {
            r.get()
          } catch (ce: CancellationException) {
            ex = ce
          } catch (ee: ExecutionException) {
            ex = ee.cause
          } catch (ie: InterruptedException) {
            // ignore/reset
            Thread.currentThread().interrupt()
          }
        }
        if (ex != null) {
          Log.error(ex) { ex.message }
        }
      }
    }
    executor.prestartCoreThread()
    executor.removeOnCancelPolicy = true
    return executor
  }

  @Bean
  @ConditionalOnMissingBean
  fun scheduler(
    scheduleService: ObjectFactory<ScheduledExecutorService>,
    executor: Executor,
    clock: Clock
  ): Scheduler {
    return DefaultScheduler(scheduleService.`object`, executor, clock)
  }

//  @Bean
//  fun taskScheduler(scheduler: Scheduler): TaskScheduler {
//    return SpringTaskScheduler(scheduler)
//  }
}
