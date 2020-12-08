package play.example

import com.typesafe.config.ConfigFactory
import kotlin.system.exitProcess
import play.*
import play.example.common.ServerMode
import play.example.module.server.event.ApplicationStartedEvent
import play.inject.getInstanceOrNull
import play.net.netty.TcpServer
import play.util.collection.UnsafeAccessor
import play.util.concurrent.LoggingUncaughtExceptionHandler

object App {
  init {
    Thread.setDefaultUncaughtExceptionHandler(LoggingUncaughtExceptionHandler)
    SystemProps.set("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager")
  }

  val serverMode: ServerMode = ServerMode.forName(SystemProps.getOrDefault("SERVER_MODE", "local"))

  @JvmStatic
  fun main(args: Array<String>) {
    UnsafeAccessor.disableWarning()
    if (sys.isWindows()) {
      setWindowsProperties()
    }

    try {
      val application = Application.start(ConfigFactory.load("$serverMode.conf"))
      Log.info { "Mode: ${application.mode}" }
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
    application.injector.getInstanceOrNull<TcpServer>(name)?.start()
  }

  val mode: Mode get() = Application.current().mode

  private fun setWindowsProperties() {
    SystemProps.setIfAbsent("MODE", Mode.Dev)
    SystemProps.setIfAbsent("SERVER_MODE", ServerMode.Local)
    SystemProps.setIfAbsent("io.netty.availableProcessors", "4")
  }
}

object AppRemote {
  @JvmStatic
  fun main(args: Array<String>) {
    SystemProps.set("SERVER_MODE", ServerMode.Remote.toString())
    App.main(args)
  }
}
