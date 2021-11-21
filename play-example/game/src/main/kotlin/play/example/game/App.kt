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
import play.SystemProps
import play.example.game.container.ContainerApp
import play.example.game.container.gs.GameServerManager
import play.net.netty.NettyServer
import play.res.ResourceManager
import play.res.ResourceReloadListener
import play.spring.getInstance
import play.spring.getInstances
import play.util.concurrent.LoggingUncaughtExceptionHandler
import play.util.concurrent.PlayPromise
import play.util.reflect.ClassScanner
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.minutes

/**
 *
 * @author LiangZengle
 */
object App {
  init {
    Thread.setDefaultUncaughtExceptionHandler(LoggingUncaughtExceptionHandler)
    SystemProps.set("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager")
  }

  @JvmStatic
  fun main(args: Array<String>) {
    Log.info { "App starting" }

    val config = loadConfig(null)
    val classScanner = newClassScanner(config)

    try {
      // 初始化策划配置
      val resourceManager = ResourceManager(config.getString("play.res.path"), classScanner)
      resourceManager.init()

      // 启动Spring
      val springApplication = SpringApplicationBuilder()
        .bannerMode(Banner.Mode.OFF)
        .sources(ContainerApp::class.java)
        .build()

      springApplication.addInitializers(
        ApplicationContextInitializer<ConfigurableApplicationContext> {
          it.beanFactory.apply {
            registerSingleton("config", config)
            registerSingleton("resourceManager", resourceManager)
            registerSingleton("classScanner", classScanner)
          }
        })
      val applicationContext = springApplication.run()

      // 注册配置监听器
      resourceManager.registerReloadListeners(applicationContext.getInstances<ResourceReloadListener>())

      // 初始化游戏服
      val initPromise = PlayPromise.make<Void>()
      applicationContext.getInstance<ActorRef<GameServerManager.Command>>()
        .tell(GameServerManager.Init(initPromise))
      // 阻塞等待初始化完成
      initPromise.future.await(5.minutes)

      // 开启网络服务
      startNettyServer(applicationContext, "gameSocketServer")
      startNettyServer(applicationContext, "adminHttpServer")

      Log.info { "App started" }
    } catch (e: Throwable) {
      Log.error(e) { "App failed to start" }
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
    applicationContext.getInstance<NettyServer>(name).start()
  }
}
