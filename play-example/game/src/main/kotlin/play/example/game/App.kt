package play.example.game

import akka.actor.typed.ActorRef
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import org.springframework.boot.Banner
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import play.Log
import play.OS
import play.SystemProps
import play.example.game.container.ContainerApp
import play.example.game.container.gs.GameServerManager
import play.net.netty.NettyServer
import play.res.ResourceManager
import play.res.ResourceReloadListener
import play.util.concurrent.LoggingUncaughtExceptionHandler
import play.util.reflect.ClassScanner
import play.util.unsafeCast
import kotlin.system.exitProcess

/**
 *
 * @author LiangZengle
 */
object App {
  init {
    LoggingUncaughtExceptionHandler.setAsDefault()
    SystemProps.set("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager")
  }

  @JvmStatic
  fun main(args: Array<String>) {
    Log.info { "App start" }
    if (OS.isWindows) {
      setWindowsProperties()
    }

    val config = loadConfig(null)
    val classScanner = newClassScanner(config)

    try {
      val resourceManager = ResourceManager(config.getString("play.res.path"), classScanner)
      resourceManager.init()

      val springApplication = SpringApplicationBuilder()
        .bannerMode(Banner.Mode.OFF)
        .sources(ContainerApp::class.java)
        .build()

      springApplication.addInitializers(
        ApplicationContextInitializer<ConfigurableApplicationContext> {
          it.beanFactory.registerSingleton("config", config)
          it.beanFactory.registerSingleton("configManager", resourceManager)
          it.beanFactory.registerSingleton("classScanner", classScanner)
        })
      val applicationContext = springApplication.run()

      val resourceReloadListeners = applicationContext.getBeansOfType(ResourceReloadListener::class.java).values
      resourceManager.registerReloadListeners(resourceReloadListeners)

      applicationContext.getBean("gameServerManager").unsafeCast<ActorRef<GameServerManager.Command>>()
        .tell(GameServerManager.Init)

      startNettyServer(applicationContext, "gameSocketServer")
      startNettyServer(applicationContext, "adminHttpServer")
    } catch (e: Throwable) {
      e.printStackTrace()
      exitProcess(-1)
    }
  }

  private fun newClassScanner(conf: Config): ClassScanner {
    val jarsToScan = conf.getStringList("play.reflection.jars-to-scan")
    val packagesToScan = conf.getStringList("play.reflection.packages-to-scan")
    return ClassScanner(jarsToScan, packagesToScan)
  }

  private fun loadConfig(priorityConf: Config?): Config {
    val referenceConf = ConfigFactory.parseResources("reference.conf")
    val applicationConf = ConfigFactory.defaultApplication()
    var config = priorityConf?.withFallback(applicationConf) ?: applicationConf
    config = config.withFallback(referenceConf).resolve()
    return config
  }

  private fun startNettyServer(applicationContext: ApplicationContext, name: String) {
    applicationContext.getBean(name, NettyServer::class.java).start()
  }

  private fun setWindowsProperties() {
    SystemProps.setIfAbsent("io.netty.availableProcessors", "4")
  }
}
