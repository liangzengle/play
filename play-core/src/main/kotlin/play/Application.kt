@file:Suppress("UnstableApiUsage")

package play

import com.typesafe.config.Config
import javax.inject.Inject
import kotlin.system.exitProcess
import play.inject.Injector
import play.scheduling.Scheduler

/**
 * Created by LiangZengle on 2020/2/15.
 */
interface Application {
  val conf: Config

  val injector: Injector

  val shutdownCoordinator: ShutdownCoordinator

  fun shutdown() {
    exitProcess(0)
  }

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
      return startWith(ApplicationBuilder())
    }

    @JvmStatic
    fun startWith(mainConfig: Config): Application {
      return startWith(ApplicationBuilder().mainConfig(mainConfig))
    }

    @JvmStatic
    fun startWith(builder: ApplicationBuilder): Application {
      synchronized(this) {
        check(current == null) { "Application is RUNNING" }
        current = builder.build()
        return current()
      }
    }
  }
}

internal class ApplicationImpl @Inject constructor(
  override val conf: Config,
  override val injector: Injector,
  override val shutdownCoordinator: ShutdownCoordinator,
  override val eventBus: ApplicationEventBus,
  override val scheduler: Scheduler
) : Application
