package play.example.game.app.module.player

import RequestDispatcher
import akka.actor.typed.ActorRef
import org.springframework.stereotype.Component
import play.codec.MessageCodec
import play.example.common.StatusCode
import play.example.game.container.net.Session
import play.mvc.*
import play.util.control.getCause
import play.util.exception.isFatal
import play.util.function.IntIntPredicate
import play.util.logging.getLogger

/**
 * 玩家请求处理
 * @author LiangZengle
 */
@Component
class PlayerRequestHandler(private val requestDispatcher: RequestDispatcher) {

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

  fun handle(commander: RequestCommander, request: Request) {
    try {
      val result = requestDispatcher.invoke(commander, request)
      onResult(commander.id, request, result)
    } catch (e: Exception) {
      onFailure(commander.id, request, e)
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
      is RequestResult.Normal<*> -> write(
        playerId,
        Response(request.header, result.code, MessageCodec.encode(result.value))
      )
      is RequestResult.Future -> {
        // TODO should pipe to actor？
        result.future.onComplete {
          if (it.isSuccess) onResult(playerId, request, it.getOrThrow())
          else onFailure(playerId, request, it.getCause())
        }
      }
      RequestResult.None -> {
      }
    }
  }

  private fun onFailure(playerId: Long, request: Request, e: Throwable) {
    logger.error(e) { "Player($playerId) ${requestDispatcher.formatToString(request)}" }
    write(playerId, Response(request.header, StatusCode.Failure.getErrorCode()))
    if (e.isFatal()) throw e
  }

  @Suppress("NOTHING_TO_INLINE")
  private inline fun write(playerId: Long, response: Response) {
    Session.write(playerId, response)
  }
}
