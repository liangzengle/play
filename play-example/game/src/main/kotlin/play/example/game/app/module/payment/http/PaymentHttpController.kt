package play.example.game.app.module.payment.http

import org.springframework.stereotype.Component
import play.example.game.app.admin.AdminHttpActionManager
import play.example.game.app.module.payment.domain.ProductType
import play.example.game.app.module.payment.event.PlayerProductOrderEvent
import play.example.game.app.module.player.event.PlayerEventBus
import play.example.game.app.module.player.exception.PlayerNotExistsException
import play.net.http.AbstractHttpController
import play.net.http.HttpResult
import play.net.http.Route
import play.util.concurrent.PlayPromise
import play.util.control.Result2

/**
 *
 * @author LiangZengle
 */
@Component
class PaymentHttpController(manager: AdminHttpActionManager, private val playerEventBus: PlayerEventBus) :
  AbstractHttpController(manager) {

  @Route("/pay")
  fun pay(playerId: Long, productType: ProductType, productId: Int): HttpResult {
    val promise = PlayPromise.make<Result2<*>>()
    playerEventBus.publish(PlayerProductOrderEvent(playerId, productType, productId, promise))
    return promise.future
      .map(::toHttpResult)
      .recover(PlayerNotExistsException::class.java) { ok(10001, it.message ?: "Player not exists") }
      .toHttpResult()
  }

  private fun toHttpResult(r: Result2<*>): HttpResult.Strict {
    return if (r.isOk()) {
      ok(0, "success")
    } else {
      ok(r.getErrorCode(), r.asErr().msg ?: "failed")
    }
  }

  private fun ok(code: Int, msg: String): HttpResult.Strict {
    return ok("""{"code":$code,"message":"$msg"}""")
  }
}
