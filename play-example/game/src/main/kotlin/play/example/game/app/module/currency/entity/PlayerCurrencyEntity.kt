package play.example.game.app.module.currency.entity

import org.eclipse.collections.api.factory.primitive.IntLongMaps
import org.eclipse.collections.api.map.primitive.IntLongMap
import play.example.game.app.module.currency.domain.CurrencyType
import play.example.game.app.module.player.entity.AbstractPlayerEntity

/**
 * 玩家的货币数据
 * @author LiangZengle
 */
class PlayerCurrencyEntity(id: Long) : AbstractPlayerEntity(id) {

  private val data = IntLongMaps.mutable.empty()

  fun data(): IntLongMap = data

  fun get(type: CurrencyType) = data.get(type.id)

  fun set(type: CurrencyType, value: Long) = data.put(type.id, value)

  fun add(type: CurrencyType, add: Long) = data.addToValue(type.id, add)

  fun reduce(type: CurrencyType, reduce: Long) = data.addToValue(type.id, reduce)
}
