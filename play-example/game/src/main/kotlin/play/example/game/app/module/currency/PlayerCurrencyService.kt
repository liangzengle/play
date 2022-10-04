package play.example.game.app.module.currency

import org.springframework.stereotype.Component
import play.example.game.app.module.currency.domain.CurrencyType
import play.example.game.app.module.currency.entity.PlayerCurrencyEntityCache
import play.example.game.app.module.player.PlayerManager

/**
 *
 * @author LiangZengle
 */
@Component
class PlayerCurrencyService(private val entityCache: PlayerCurrencyEntityCache) {

  fun isEnough(self: PlayerManager.Self, currencyType: CurrencyType, expected: Long): Boolean {
    return getCurrency(self, currencyType) >= expected
  }

  fun getCurrency(self: PlayerManager.Self, currencyType: CurrencyType) =
    entityCache.getOrCreate(self.id).get(currencyType)

  fun addCurrency(self: PlayerManager.Self, currencyType: CurrencyType, add: Long): Long {
    return entityCache.getOrCreate(self.id).add(currencyType, add)
  }

  fun reduceCurrency(self: PlayerManager.Self, currencyType: CurrencyType, reduce: Long): Long {
    require(isEnough(self, currencyType, reduce)) { "${self}的${currencyType}不足" }
    return reduceCurrencyNoCheck(self, currencyType, reduce)
  }

  fun reduceCurrencyNoCheck(self: PlayerManager.Self, currencyType: CurrencyType, reduce: Long): Long {
    return entityCache.getOrCreate(self.id).reduce(currencyType, reduce)
  }
}
