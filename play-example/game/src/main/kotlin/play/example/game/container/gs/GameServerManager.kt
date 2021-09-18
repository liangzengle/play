package play.example.game.container.gs

import akka.actor.typed.ActorRef
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import com.typesafe.config.Config
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList
import org.springframework.context.ConfigurableApplicationContext
import play.akka.AbstractTypedActor
import play.example.game.container.db.ContainerRepositoryProvider
import play.example.game.container.gs.entity.GameServerEntity
import play.scala.toPlay
import play.util.concurrent.PlayPromise
import play.util.control.getCause
import play.util.forEach
import play.util.unsafeCast
import scala.concurrent.Promise
import java.time.Duration

/**
 *
 * @author LiangZengle
 */
class GameServerManager(
  context: ActorContext<Command>,
  private val parentApplicationContext: ConfigurableApplicationContext,
  private val containerRepositoryProvider: ContainerRepositoryProvider
) : AbstractTypedActor<GameServerManager.Command>(context) {
  interface Command
  object Init : Command
  data class CreateGameServer(val serverId: Int, val promise: PlayPromise<Int>) : Command
  private class CreateGameServerResult(val serverId: Int, val result: Result<Unit>) : Command
  data class StartGameServer(val serverId: Int, val promise: PlayPromise<Int>) : Command
  data class CloseGameServer(val serverId: Int) : Command

  private var gameServerIds = IntArrayList()

  override fun createReceive(): Receive<Command> {
    return newReceiveBuilder()
      .accept(::init)
      .accept(::createGameServer)
      .accept(::startGameServer)
      .accept(::onCreateGameServerResult)
      .accept(::closeGameServer)
      .build()
  }

  private fun init(cmd: Init) {
    val repository = containerRepositoryProvider.get()
    repository
      .listIds(GameServerEntity::class.java)
      .onSuccess { serverIds ->
        if (serverIds.isEmpty()) {
          val conf = parentApplicationContext.getBean(Config::class.java)
          val initialServerId = conf.getInt("play.initial-game-server-id")
          self.tell(CreateGameServer(initialServerId, PlayPromise.make()))
        } else {
          serverIds.forEach(gameServerIds::add)
          serverIds.forEach { serverId ->
            self.tell(StartGameServer(serverId, PlayPromise.make()))
          }
        }
      }.await(Duration.ofSeconds(10))
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
      containerRepositoryProvider.get()
        .insert(GameServerEntity(serverId))
        .onComplete(
          {
            logger.info("GameServerEntity($serverId)创建成功")
            self.tell(StartGameServer(serverId, promise))
          },
          {
            logger.error("GameServerEntity($serverId)创建失败", it)
            promise.failure(it)
          }
        )
    } else {
      self.tell(StartGameServer(serverId, promise))
    }
  }

  private fun onCreateGameServerResult(cmd: CreateGameServerResult) {
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
    val app = context.spawn(
      Behaviors.setup<GameServerActor.Command> { ctx ->
        GameServerActor(
          ctx,
          self,
          serverId,
          parentApplicationContext
        )
      },
      serverId.toString()
    )
    val promise = Promise.apply<Unit>()
    app.tell(GameServerActor.Start(promise))
    cmd.promise.completeWith(promise.future().toPlay().map { 0 })
    promise.future().pipToSelf { CreateGameServerResult(serverId, it) }
  }

  private fun closeGameServer(cmd: CloseGameServer) {
    context.getChild(cmd.serverId.toString())
      .forEach {
        it.unsafeCast<ActorRef<GameServerActor.Command>>().tell(GameServerActor.Stop)
      }
  }
}
