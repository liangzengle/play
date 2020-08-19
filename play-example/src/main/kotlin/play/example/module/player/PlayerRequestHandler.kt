package play.example.module.player

import ControllerInvokerManager
import akka.actor.typed.ActorRef
import play.example.common.net.SessionActor
import play.example.module.StatusCode
import play.example.module.friend.FriendManager
import play.getLogger
import play.mvc.Request
import play.mvc.RequestResult
import play.mvc.Response
import play.util.exception.isFatal
import play.util.function.IntIntPredicate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 玩家请求处理
 * @author LiangZengle
 */
@Singleton
class PlayerRequestHandler @Inject constructor(private val controllerInvokerManager: ControllerInvokerManager) {

  private val logger = getLogger()

  private val handlerActor = ArrayList<Pair<ActorRef<PlayerRequest>, IntIntPredicate>>(4)

  fun register(actor: ActorRef<PlayerRequest>, predicate: IntIntPredicate) {
    synchronized(handlerActor) {
      handlerActor.add(actor to predicate)
    }
  }

  fun findHandlerActor(request: Request): ActorRef<PlayerRequest>? {
    val moduleId = request.header.moduleId.toInt()
    val cmd = request.header.cmd.toInt()
    for (i in handlerActor.indices) {
      val (actor, predicate) = handlerActor[i]
      if (predicate.test(moduleId, cmd)) {
        return actor
      }
    }
    return null
  }

  fun handle(req: PlayerRequest) {
    try {
      val result = controllerInvokerManager.invoke(req.playerId, req.request)
      onResult(req.playerId, req.request, result)
    } catch (e: Exception) {
      onFailure(req.playerId, req.request, e)
    }
  }

  fun handle(self: Self, request: Request) {
    try {
      val result = controllerInvokerManager.invoke(self, request)
      onResult(self.id, request, result)
    } catch (e: Exception) {
      onFailure(self.id, request, e)
    }
  }

  private fun onResult(playerId: Long, request: Request, result: RequestResult<*>?) {
    if (result == null) {
      logger.warn { "Player(${playerId})的请求无法处理: $request" }
      return
    }
    when (result) {
      is RequestResult.Code -> write(playerId, Response(request.header, result.code))
      is RequestResult.Ok<*> -> write(playerId, Response(request.header, 0, result.value))
      is RequestResult.Future -> {
        result.future.onComplete {
          if (it.isSuccess) onResult(playerId, request, it.get())
          else onFailure(playerId, request, it.cause)
        }
      }
      RequestResult.Nothing -> {
      }
    }
  }

  private fun onFailure(playerId: Long, request: Request, e: Throwable) {
    logger.error(e) { "Player($playerId) ${controllerInvokerManager.format(request)}" }
    write(playerId, Response(request.header, StatusCode.Failure))
    if (e.isFatal()) throw e
  }

  @Suppress("NOTHING_TO_INLINE")
  private inline fun write(playerId: Long, response: Response) {
    SessionActor.write(playerId, response)
  }
}

data class PlayerRequest(@JvmField val playerId: Long, @JvmField val request: Request) : FriendManager.Command
