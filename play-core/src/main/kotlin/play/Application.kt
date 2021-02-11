@file:Suppress("UnstableApiUsage")

package play

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import javax.inject.Inject
import play.inject.Injector
import play.util.reflect.createInstance
import play.util.scheduling.Scheduler

/**
 * Created by LiangZengle on 2020/2/15.
 */
interface Application {
  val conf: Configuration

  val mode: Mode

  val injector: Injector

  val lifecycle: ApplicationLifecycle

  fun stop() = lifecycle.stop()

  fun pid(): Long = ProcessHandle.current().pid()

  val eventBus: ApplicationEventBus

  val scheduler: Scheduler

  companion object {

    private var current: Application? = null

    @JvmStatic
    fun current(): Application {
      return current ?: throw IllegalStateException("Application not started.")
    }

    @JvmStatic
    fun start(): Application {
      return start(ConfigFactory.empty())
    }

    @JvmStatic
    fun start(setting: Config): Application {
      synchronized(this) {
        check(current == null) { "Application is RUNNING" }
        val referenceConf = ConfigFactory.parseResources("reference.conf")
        val applicationConf =
          ConfigFactory.defaultApplication().withFallback(setting).withFallback(referenceConf).resolve()
        val conf = (applicationConf + referenceConf).getConfig("app").toConfiguration()
        val mode = Mode.forName(conf.getString("mode"))
        ModeDependent.setMode(mode)
        val packagesToScan = conf.getStringList("reflection.packages-to-scan")
        val classScanner = ClassScanner(packagesToScan)
        val lifecycle = DefaultApplicationLifecycle()
        val context = ApplicationLoader.Context(conf, mode, classScanner, lifecycle)
        val applicationLoader = conf.getClass<ApplicationLoader>("loader").createInstance()
        current = applicationLoader.load(context)
        return current()
      }
    }
  }
}

class DefaultApplication @Inject constructor(
  override val conf: Configuration,
  override val mode: Mode,
  override val injector: Injector,
  override val lifecycle: ApplicationLifecycle,
  override val eventBus: ApplicationEventBus,
  override val scheduler: Scheduler
) : Application
