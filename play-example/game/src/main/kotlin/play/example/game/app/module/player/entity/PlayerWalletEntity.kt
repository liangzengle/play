package play.example.game.app.module.player.entity

import org.eclipse.collections.impl.factory.primitive.IntLongMaps

/**
 * 玩家钱包
 *
 * @author LiangZengle
 */
class PlayerWalletEntity(id: Long) : AbstractPlayerEntity(id) {

  /**
   * 货币类型:余额
   */
  private val balances = IntLongMaps.mutable.empty()

  fun getValue(type: Int) = balances[type]
}
