package play.example

import com.typesafe.config.ConfigFactory
import play.*
import play.example.common.ServerMode
import play.inject.instanceOf
import play.net.netty.TcpServer
import kotlin.system.exitProcess

object App {

  val serverMode: ServerMode = ServerMode.forName(SystemProperties.getOrDefault("SERVER_MODE", "local"))

  @JvmStatic
  fun main(args: Array<String>) {
    if (sys.isWindows()) {
      setWindowsProperties()
    }

    val application: Application
    try {
      application = Application.start(ConfigFactory.load("$serverMode.conf"))
      Log.info { "mode: ${application.mode}" }
      Log.info { "serverMode: $serverMode" }
      val gameServer = application.injector.instanceOf<TcpServer>("game")
      gameServer.start()
    } catch (e: Throwable) {
      e.printStackTrace()
      exitProcess(-1)
    }
  }

  val mode: Mode get() = Application.current().mode

  private fun setWindowsProperties() {
    SystemProperties.setIfAbsent("MODE", Mode.Dev)
    SystemProperties.setIfAbsent("SERVER_MODE", ServerMode.Local)
  }
}

object AppRemote {
  @JvmStatic
  fun main(args: Array<String>) {
    SystemProperties.set("SERVER_MODE", ServerMode.Remote.toString())
    App.main(args)
  }
}
