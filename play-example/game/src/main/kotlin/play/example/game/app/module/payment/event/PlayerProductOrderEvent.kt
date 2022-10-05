package play.example.game.app.module.payment.event

import play.example.game.app.module.payment.domain.ProductType
import play.example.game.app.module.player.event.PromisedPlayerEvent
import play.util.concurrent.PlayPromise
import play.util.control.Result2

/**
 *
 * @author LiangZengle
 */
class PlayerProductOrderEvent(
  override val playerId: Long,
  val productType: ProductType,
  val productId: Int,
  override val promise: PlayPromise<Result2<*>>
) : PromisedPlayerEvent<Result2<*>>
