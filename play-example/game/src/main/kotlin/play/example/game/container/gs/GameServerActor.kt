package play.example.game.container.gs

import RequestDispatcher
import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.PostStop
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import com.google.common.primitives.Bytes
import com.typesafe.config.Config
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.boot.Banner
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import play.akka.AbstractTypedActor
import play.akka.logging.ActorMDC
import play.db.DatabaseNameProvider
import play.example.common.id.UIDGenerator
import play.example.game.app.GameApp
import play.example.game.app.module.platform.domain.Platforms
import play.example.game.app.module.server.res.ServerConfig
import play.example.game.container.gs.domain.GameServerId
import play.res.ResourceManager
import play.res.ResourceReloadListener
import play.scala.toPlay
import play.scheduling.ManagedScheduler
import play.scheduling.Scheduler
import play.scheduling.SpringTaskScheduler
import play.spring.*
import play.util.classOf
import play.util.concurrent.PlayPromise
import play.util.concurrent.Promise
import play.util.logging.withMDC
import play.util.unsafeCast
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
    val messageType: Class<T>,
    val promise: PlayPromise<ActorRef<T>>
  ) : Command

  class Start(val promise: Promise<Unit>) : Command
  class Stop(val promise: Promise<Unit>) : Command
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
    val behavior =
      Behaviors.withMdc(cmd.messageType, actorMdc.staticMdc, actorMdc.mdcPerMessage(), cmd.behaviorFactory(actorMdc))
    val ref = context.spawn(behavior, cmd.name)
    cmd.promise.success(ref)
  }

  private fun start(cmd: Start): Behavior<Command> {
    val future = future {
      withMDC(actorMdc.staticMdc, ::startApplicationContext)
    }
    cmd.promise.completeWith(future.toPlay())
    future.pipToSelf { StartResult(it.exceptionOrNull()) }
    return starting()
  }

  private fun startApplicationContext() {
    val springApplication = SpringApplicationBuilder()
      .bannerMode(Banner.Mode.OFF)
      .parent(parentApplicationContext)
      .sources(classOf<GameApp>())
      .contextFactory(PlayNonWebApplicationContextFactory())
      .registerShutdownHook(false)
      .build()
    val conf = parentApplicationContext.getInstance<Config>()
    val dbNamePattern = conf.getString("play.db.name-pattern")
    val dbName = String.format(dbNamePattern, serverId)

    val scheduler = parentApplicationContext.getInstance<Scheduler>()
    val managedScheduler = ManagedScheduler(scheduler)
    val taskScheduler = SpringTaskScheduler(managedScheduler)
    // TODO
    val platformNames = listOf("Dev")
    val platformIdArray = ByteArray(platformNames.size)
    for (i in platformNames.indices) {
      platformIdArray[i] = Platforms.getOrThrow(platformNames[i]).toByte()
    }
    val serverConfig = ServerConfig(serverId.toShort(), Bytes.asList(*platformIdArray))

    springApplication.addInitializers(
      ApplicationContextInitializer<ConfigurableApplicationContext> {
        it.unsafeCast<BeanDefinitionRegistry>().apply {
          registerBeanDefinition("gameServerActor", beanDefinition(typeOf<ActorRef<Command>>(), self))
          registerBeanDefinition("requestDispatcher", beanDefinition(typeOf<RequestDispatcher>()))
        }
        it.beanFactory.apply {
          registerSingleton("actorMdc", actorMdc)
          registerSingleton("gameServerId", GameServerId(serverId))
          registerSingleton("serverConfig", serverConfig)
          registerSingleton("databaseNameProvider", DatabaseNameProvider { dbName })
          registerSingleton("uidGenerator", UIDGenerator(serverId))
          registerSingleton("taskScheduler", taskScheduler)
          registerSingleton("scheduler", managedScheduler)
        }
      }
    )
    applicationContext = springApplication.run()
    val resourceManager = applicationContext.getInstance<ResourceManager>()
    val resourceReloadListeners = applicationContext.getInstances<ResourceReloadListener>()
    resourceManager.registerReloadListeners(resourceReloadListeners)
  }

  private fun stop(cmd: Stop): Behavior<Command> {
    log.info("Stop game server: {}", serverId)
    cmd.promise.success(Unit)
    return stoppedBehavior()
  }

  private fun postStop(signal: PostStop) {
    if (this::applicationContext.isInitialized) {
      applicationContext.closeAndWait()
    }
    log.info("Game server [{}] stopped.", serverId)
  }
}
