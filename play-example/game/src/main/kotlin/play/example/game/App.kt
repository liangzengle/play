package play.example

import akka.actor.typed.ActorSystem
import com.typesafe.config.ConfigFactory
import kotlin.system.exitProcess
import play.*
import play.example.common.ServerMode
import play.example.game.module.server.event.ApplicationStartedEvent
import play.inject.getInstance
import play.net.netty.NettyServer
import play.util.collection.UnsafeAccessor
import play.util.concurrent.LoggingUncaughtExceptionHandler

object App {
  init {
    Thread.setDefaultUncaughtExceptionHandler(LoggingUncaughtExceptionHandler)
    SystemProps.set("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager")
  }

  val serverMode: ServerMode = SystemProps.getOrNull("SERVER_MODE")?.let(ServerMode::forName) ?: ServerMode.Game

  @JvmStatic
  fun main(args: Array<String>) {
    UnsafeAccessor.disableWarning()
    if (sys.isWindows) {
      setWindowsProperties()
    }

    try {
      val application = Application.startWith(ConfigFactory.load("$serverMode.conf"))
      Log.info { "ServerMode: $serverMode" }
      application.eventBus.postBlocking(ApplicationStartedEvent)
      startTcpServer(application, "game")
      startTcpServer(application, "admin-http")
    } catch (e: Throwable) {
      e.printStackTrace()
      exitProcess(-1)
    }
  }

  private fun startTcpServer(application: Application, name: String) {
    application.injector.getInstance<NettyServer>(name).start()
  }

  private fun setWindowsProperties() {
    SystemProps.setIfAbsent("MODE", Mode.Dev)
    SystemProps.setIfAbsent("io.netty.availableProcessors", "4")
  }
}
