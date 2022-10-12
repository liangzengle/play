package play.example.game.app.module.payment

import org.springframework.stereotype.Component
import play.example.game.app.module.payment.event.PlayerProductOrderEvent
import play.example.game.app.module.player.PlayerManager
import play.example.game.app.module.player.event.PlayerEventBus
import play.example.game.app.module.player.event.subscribe
import play.util.control.ok

/**
 *
 * @author LiangZengle
 */
@Component
class PaymentService(eventBus: PlayerEventBus) {

  init {
    eventBus.subscribe(::onPay)
  }

  fun onPay(self: PlayerManager.Self, event: PlayerProductOrderEvent) {
    val promise = event.promise
    promise.catchingComplete {
      // TODO
      ok(0)
    }
  }
}
