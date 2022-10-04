package play.example.game.app.module.currency.domain

import play.example.common.ModularCode
import play.example.common.StatusCode
import play.example.game.app.module.ModuleId

/**
 *
 * @author LiangZengle
 */
@ModularCode
object CurrencyStatusCode : StatusCode(ModuleId.Currency) {
  val GoldNotEnough = code(1)
}
