package play.example.module.player

import ControllerInvokerManager
import akka.actor.typed.ActorRef
import javax.inject.Inject
import javax.inject.Singleton
import play.example.common.net.SessionActor
import play.example.module.StatusCode
import play.getLogger
import play.mvc.*
import play.util.control.getCause
import play.util.exception.isFatal
import play.util.function.IntIntPredicate

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

  fun handle(req: AbstractPlayerRequest) {
    handle(req.playerId, req.request)
  }

  fun handle(playerId: Long, request: Request) {
    try {
      val result = controllerInvokerManager.invoke(playerId, request)
      onResult(playerId, request, result)
    } catch (e: Exception) {
      onFailure(playerId, request, e)
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

  fun onResult(req: AbstractPlayerRequest, result: RequestResult<*>?) {
    onResult(req.playerId, req.request, result)
  }

  private fun onResult(playerId: Long, request: Request, result: RequestResult<*>?) {
    if (result == null) {
      logger.warn { "Player($playerId)的请求无法处理: $request" }
      return
    }
    when (result) {
      is RequestResult.Code -> write(playerId, Response(request.header, result.code))
      is RequestResult.Ok<*> -> write(
        playerId,
        Response(request.header, 0, if (result.value is Unit) null else result.value)
      )
      is RequestResult.Future -> {
        result.future.onComplete {
          if (it.isSuccess) onResult(playerId, request, it.getOrThrow())
          else onFailure(playerId, request, it.getCause())
        }
      }
      RequestResult.Nothing -> {
      }
    }
  }

  private fun onFailure(playerId: Long, request: Request, e: Throwable) {
    logger.error(e) { "Player($playerId) ${controllerInvokerManager.formatToString(request)}" }
    write(playerId, Response(request.header, StatusCode.Failure.getErrorCode()))
    if (e.isFatal()) throw e
  }

  @Suppress("NOTHING_TO_INLINE")
  private inline fun write(playerId: Long, response: Response) {
    SessionActor.write(playerId, response)
  }
}
