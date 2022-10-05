package play.example.game.container.gs

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.PostStop
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import com.typesafe.config.Config
import org.eclipse.collections.impl.factory.primitive.IntSets
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList
import org.springframework.context.ConfigurableApplicationContext
import play.akka.AbstractTypedActor
import play.akka.logging.ActorMDC
import play.db.Repository
import play.example.game.container.gs.entity.GameServerEntity
import play.spring.getInstance
import play.util.concurrent.Future.Companion.toPlay
import play.util.concurrent.PlayPromise
import play.util.control.getCause
import play.util.unsafeCast
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import kotlin.concurrent.thread
import kotlin.system.exitProcess

/**
 *
 * @author LiangZengle
 */
class GameServerManager(
  context: ActorContext<Command>,
  private val parentApplicationContext: ConfigurableApplicationContext,
  private val repository: Repository,
) : AbstractTypedActor<GameServerManager.Command>(context) {
  interface Command
  class Init(val promise: PlayPromise<Any?>) : Command
  private class InitResult(val result: Result<*>) : Command
  data class CreateGameServer(val serverId: Int, val promise: PlayPromise<Int>) : Command
  private class GameServerStartResult(val serverId: Int, val result: Result<Unit>) : Command
  data class StartGameServer(val serverId: Int, val promise: PlayPromise<Int>) : Command
  data class CloseGameServer(val serverId: Int, val promise: PlayPromise<Int>) : Command

  private var gameServerIds = IntArrayList()

  override fun createReceive(): Receive<Command> {
    return newReceiveBuilder()
      .accept(::init)
      .accept(::onInitResult)
      .accept(::createGameServer)
      .accept(::startGameServer)
      .accept(::onGameServerStartResult)
      .accept(::closeGameServer)
      .acceptSignal(::postStop)
      .build()
  }

  private fun postStop(signal: PostStop) {
    thread { exitProcess(-1) }
  }

  private fun init(cmd: Init) {
    val future = repository
      .queryIds(GameServerEntity::class.java)
      .collect({ IntSets.mutable.empty() }, { set, id -> set.add(id) })
      .flatMapMany { serverIds ->
        if (serverIds.isEmpty) {
          val conf = parentApplicationContext.getInstance<Config>()
          val initialServerId = conf.getInt("play.initial-game-server-id")
          val promise = PlayPromise.make<Int>()
          self.tell(CreateGameServer(initialServerId, promise))
          gameServerIds.add(initialServerId)
          Mono.fromFuture(promise.future.toJava()).flux()
        } else {
          Flux.fromIterable(
            serverIds.asLazy().collect { serverId ->
              val promise = PlayPromise.make<Int>()
              self.tell(StartGameServer(serverId, promise))
              gameServerIds.add(serverId)
              Mono.fromFuture(promise.future.toJava())
            }
          )
        }
      }
      .ignoreElements().toFuture().toPlay()
    future.pipToSelf(::InitResult)
    cmd.promise.completeWith(future)
  }

  private fun onInitResult(initResult: InitResult): Behavior<Command> {
    val result = initResult.result
    return if (result.isFailure) {
      log.error("服务器启动失败", result.getCause())
      stoppedBehavior()
    } else {
      log.info("服务器启动成功")
      sameBehavior()
    }
  }

  private fun createGameServer(cmd: CreateGameServer) {
    val logger = context.log
    val serverId = cmd.serverId
    val promise = cmd.promise

    val name = serverId.toString()
    if (context.getChild(name).isPresent) {
      promise.failure(IllegalStateException("GameServer($serverId) is started."))
      return
    }

    if (!gameServerIds.contains(serverId)) {
      repository
        .insert(GameServerEntity(serverId))
        .onSuccess {
          logger.info("GameServerEntity($serverId)创建成功")
          self.tell(StartGameServer(serverId, promise))
        }
        .onFailure {
          logger.error("GameServerEntity($serverId)创建失败", it)
          promise.failure(it)
        }
    } else {
      self.tell(StartGameServer(serverId, promise))
    }
  }

  private fun onGameServerStartResult(cmd: GameServerStartResult) {
    val logger = context.log
    val serverId = cmd.serverId
    if (cmd.result.isSuccess) {
      logger.info("游戏服启动成功: $serverId")
    } else {
      logger.error("游戏服启动失败: $serverId", cmd.result.getCause())
    }
  }

  private fun startGameServer(cmd: StartGameServer) {
    val serverId = cmd.serverId
    val actorMDC = ActorMDC(mapOf("serverId" to serverId.toString()))
    val behavior = Behaviors.withMdc(
      GameServerActor.Command::class.java,
      actorMDC.staticMdc,
      actorMDC.mdcPerMessage(),
      Behaviors.setup { ctx ->
        GameServerActor(
          ctx,
          self,
          serverId,
          parentApplicationContext,
          actorMDC
        )
      }
    )
    val app = context.spawn(behavior, serverId.toString())
    val promise = cmd.promise
    val startPromise = PlayPromise.make<Unit>()
    app.tell(GameServerActor.Start(startPromise))
    startPromise.future
      .onComplete {
        self.tell(GameServerStartResult(serverId, it))
        promise.complete(it.map { serverId })
      }
  }

  private fun closeGameServer(cmd: CloseGameServer) {
    val serverId = cmd.serverId
    val promise = cmd.promise

    val maybeChild = context.getChild(serverId.toString())
    if (maybeChild.isEmpty) {
      promise.success(-1)
      return
    }
    val child = maybeChild.get().unsafeCast<ActorRef<GameServerActor.Command>>()
    val stopPromise = PlayPromise.make<Unit>()
    child.tell(GameServerActor.Stop(stopPromise))
    promise.completeWith(stopPromise.future.map { serverId })
  }
}
