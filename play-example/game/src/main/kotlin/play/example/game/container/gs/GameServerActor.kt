package play.example.game.container.gs

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.PostStop
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Receive
import com.google.common.primitives.Bytes
import com.typesafe.config.Config
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.boot.Banner
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import play.akka.AbstractTypedActor
import play.akka.stoppedBehavior
import play.db.DatabaseNameProvider
import play.example.game.app.GameApp
import play.example.game.app.module.platform.domain.Platform
import play.example.game.app.module.server.res.ServerConfig
import play.example.game.container.gs.domain.GameServerId
import play.example.game.container.gs.logging.ActorMDC
import play.res.ResourceManager
import play.res.ResourceReloadListener
import play.scheduling.ManagedScheduler
import play.scheduling.Scheduler
import play.spring.PlayNonWebApplicationContextFactory
import play.spring.rootBeanDefinition
import play.util.classOf
import play.util.concurrent.PlayPromise
import play.util.logging.withMDC
import play.util.unsafeCast
import scala.concurrent.Promise
import kotlin.reflect.typeOf

/**
 *
 * @author LiangZengle
 */
class GameServerActor(
  ctx: ActorContext<Command>,
  parent: ActorRef<GameServerManager.Command>,
  val serverId: Int,
  val parentApplicationContext: ConfigurableApplicationContext,
  val actorMdc: ActorMDC
) : AbstractTypedActor<GameServerActor.Command>(ctx) {

  interface Command
  data class Spawn<T>(
    val behaviorFactory: (ActorMDC) -> Behavior<T>,
    val name: String,
    val promise: PlayPromise<ActorRef<T>>
  ) : Command

  class Start(val promise: Promise<Unit>) : Command
  object Stop : Command
  private class StartResult(val ex: Throwable?) : Command

  private lateinit var applicationContext: ConfigurableApplicationContext

  override fun createReceive(): Receive<Command> {
    return newReceiveBuilder()
      .accept(::spawn)
      .accept(::start)
      .accept(::stop)
      .acceptSignal(::postStop)
      .build()
  }

  private fun waitingStart(): Receive<Command> {
    return newReceiveBuilder().accept(::start).build()
  }

  private fun starting(): Receive<Command> {
    return newReceiveBuilder()
      .accept(::onStartResult)
      .accept(::spawn)
      .build()
  }

  private fun started(): Receive<Command> {
    return newReceiveBuilder()
      .accept(::spawn)
      .accept(::stop)
      .acceptSignal(::postStop)
      .build()
  }

  private fun onStartResult(result: StartResult): Behavior<Command> {
    val exception = result.ex
    if (exception != null) {
      log.error("Game Server [$serverId] starts FAILED!!!", exception)
      return stoppedBehavior()
    }
    log.info("Game Server [{}] starts successfully", serverId)
    return started()
  }

  private fun spawn(cmd: Spawn<Any>) {
    val behavior = cmd.behaviorFactory(actorMdc)
    val ref = context.spawn(behavior, cmd.name)
    cmd.promise.success(ref)
  }

  private fun start(cmd: Start): Behavior<Command> {
    val future = future {
      withMDC(actorMdc.staticMdc, ::startApplicationContext)
    }
    cmd.promise.completeWith(future)
    future.pipToSelf { StartResult(it.exceptionOrNull()) }
    return starting()
  }

  private fun startApplicationContext() {
    val springApplication = SpringApplicationBuilder()
      .bannerMode(Banner.Mode.OFF)
      .parent(parentApplicationContext)
      .sources(classOf<GameApp>())
      .contextFactory(PlayNonWebApplicationContextFactory())
      .build()
    val conf = parentApplicationContext.getBean(Config::class.java)
    val dbNamePattern = conf.getString("play.db.name-pattern")
    val dbName = String.format(dbNamePattern, serverId)

    val scheduler = parentApplicationContext.getBean(Scheduler::class.java)
    val managedScheduler = ManagedScheduler(scheduler)
    // TODO
    val platformNames = listOf("Dev")
    val platformIdArray = ByteArray(platformNames.size)
    for (i in platformNames.indices) {
      platformIdArray[i] = Platform.getOrThrow(platformNames[i]).getId()
    }
    val serverConfig = ServerConfig(serverId.toShort(), Bytes.asList(*platformIdArray))

    springApplication.addInitializers(
      ApplicationContextInitializer<ConfigurableApplicationContext> {
        it.unsafeCast<BeanDefinitionRegistry>()
          .apply {
            registerBeanDefinition("gameServerActor", rootBeanDefinition(typeOf<ActorRef<Command>>(), self))
            registerBeanDefinition("scheduler", rootBeanDefinition(typeOf<Scheduler>(), managedScheduler))
          }

        it.beanFactory.registerSingleton("appActorMdc", actorMdc)
        it.beanFactory.registerSingleton("gameServerId", GameServerId(serverId))
        it.beanFactory.registerSingleton("serverConfig", serverConfig)
        it.beanFactory.registerSingleton("dbNameProvider", DatabaseNameProvider { dbName })
      }
    )
    applicationContext = springApplication.run()
    val resourceManager = applicationContext.getBean(ResourceManager::class.java)
    val resourceReloadListeners = applicationContext.getBeansOfType(ResourceReloadListener::class.java).values
    resourceManager.registerReloadListeners(resourceReloadListeners)
  }

  private fun stop(cmd: Stop): Behavior<Command> {
    log.info("Stop game server: {}", serverId)
    return stoppedBehavior()
  }

  private fun postStop(signal: PostStop) {
    if (this::applicationContext.isInitialized) {
      applicationContext.close()
    }
    log.info("Game server [{}] stopped.", serverId)
  }
}
