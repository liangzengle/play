package play.example.game

import akka.actor.typed.ActorRef
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import org.apache.logging.log4j.LogManager
import org.springframework.boot.Banner
import org.springframework.boot.SpringApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import play.DefaultGracefullyShutdown
import play.GracefullyShutdown
import play.Log
import play.db.DatabaseNameProvider
import play.example.common.App
import play.example.game.container.ContainerApplication
import play.example.game.container.gs.GameServerManager
import play.net.netty.NettyServer
import play.res.ResourceManager
import play.spring.closeAndWait
import play.spring.getInstance
import play.util.concurrent.Future
import play.util.concurrent.PlayPromise
import play.util.reflect.ClassgraphClassScanner
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.minutes

/**
 *
 * @author LiangZengle
 */
object ContainerApp : App() {

  @JvmStatic
  fun main(args: Array<String>) {
    Log.info { "App starting" }

    try {
      // 启动Spring容器
      val applicationContext = startSpringApplication(args)
      // 初始化游戏服
      startGameServers(applicationContext).await(5.minutes)
      // 开启网络服务
      startNetworkService(applicationContext)

      Log.info { "App started" }
    } catch (e: Throwable) {
      Log.error(e) { "App failed to start" }
      e.printStackTrace()
      exitProcess(-1)
    }
  }

  private fun startGameServers(ctx: ApplicationContext): Future<*> {
    val initPromise = PlayPromise.make<Any?>()
    ctx.getInstance<ActorRef<GameServerManager.Command>>().tell(GameServerManager.Init(initPromise))
    return initPromise.future
  }

  private fun startSpringApplication(
    args: Array<String>
  ): ApplicationContext {
    val springApplication = buildSpringApplication()
    val applicationContext = springApplication.run(*args)
    applicationContext.getBean(GracefullyShutdown::class.java)
      .addTask(
        GracefullyShutdown.PHASE_SHUTDOWN_APPLICATION_CONTEXT,
        "shutdown Spring Application Context",
        applicationContext
      ) { Future { it.closeAndWait() } }
    return applicationContext
  }

  private fun buildSpringApplication(): SpringApplication {
    val config = loadConfig(null)
    val classScanner = newClassScanner(config)
    val resourceManager = ResourceManager(config.getString("play.res.path"), classScanner)
    val phases = GracefullyShutdown.phaseFromConfig(config.getConfig("play.shutdown"))
    val shutdown = DefaultGracefullyShutdown("App", phases, true, LogManager::shutdown)
    val dbName = config.getString("play.container.db")
    val databaseNameProvider = DatabaseNameProvider { dbName }

    resourceManager.init()

    // 启动Spring
    val springApplication = SpringApplicationBuilder()
      .bannerMode(Banner.Mode.OFF)
      .sources(ContainerApplication::class.java)
      .registerShutdownHook(false)
      .build()

    springApplication.addInitializers(ApplicationContextInitializer<ConfigurableApplicationContext> {
      it.beanFactory.apply {
        registerSingleton("config", config)
        registerSingleton("databaseNameProvider", databaseNameProvider)
        registerSingleton("resourceManager", resourceManager)
        registerSingleton("classScanner", classScanner)
        registerSingleton("shutdownPhases", phases)
        registerSingleton("gracefullyShutdown", shutdown)
      }
    })
    return springApplication
  }

  private fun newClassScanner(conf: Config): ClassgraphClassScanner {
    val jarsToScan = conf.getStringList("play.reflection.jars-to-scan")
    val packagesToScan = conf.getStringList("play.reflection.packages-to-scan")
    return ClassgraphClassScanner(jarsToScan, packagesToScan)
  }

  private fun loadConfig(priorityConf: Config?): Config {
    val referenceConf = ConfigFactory.parseResources("reference.conf")
    val applicationConf = ConfigFactory.defaultApplication()
    var config = priorityConf?.withFallback(applicationConf) ?: applicationConf
    config = config.withFallback(referenceConf).resolve()
    return config
  }

  private fun startNetworkService(applicationContext: ApplicationContext) {
    val adminHttpServer = applicationContext.getInstance<NettyServer>("adminHttpServer")
    val gameSocketServer = applicationContext.getInstance<NettyServer>("gameSocketServer")
    val shutdown = applicationContext.getInstance<GracefullyShutdown>()
    shutdown.addTask(
      GracefullyShutdown.PHASE_SHUTDOWN_NETWORK_SERVICE,
      "shutdown game socket server",
      gameSocketServer
    ) { it.stopAsync() }

    shutdown.addTask(
      GracefullyShutdown.PHASE_SHUTDOWN_NETWORK_SERVICE,
      "shutdown admin http server",
      adminHttpServer
    ) { it.stopAsync() }

    adminHttpServer.start()
    gameSocketServer.start()
  }
}
