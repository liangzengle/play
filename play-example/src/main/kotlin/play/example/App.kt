package play.example

import com.typesafe.config.ConfigFactory
import play.*
import play.example.common.ServerMode
import play.inject.getInstanceOrNull
import play.net.netty.TcpServer
import play.util.collection.UnsafeAccessor
import kotlin.system.exitProcess

object App {

  val serverMode: ServerMode = ServerMode.forName(SystemProperties.getOrDefault("SERVER_MODE", "local"))

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
    SystemProperties.setIfAbsent("MODE", Mode.Dev)
    SystemProperties.setIfAbsent("SERVER_MODE", ServerMode.Local)
    SystemProperties.setIfAbsent("io.netty.availableProcessors", "4")
  }
}

object AppRemote {
  @JvmStatic
  fun main(args: Array<String>) {
    SystemProperties.set("SERVER_MODE", ServerMode.Remote.toString())
    App.main(args)
  }
}
