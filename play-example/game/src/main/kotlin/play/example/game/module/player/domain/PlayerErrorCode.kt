package play.example.game.module.player.domain

import play.example.common.ModularCode
import play.example.common.StatusCode
import play.example.game.module.ModuleId

/**
 * 玩家模块错误码
 * @author LiangZengle
 */
@Suppress("MayBeConstant")
@ModularCode
object PlayerErrorCode : StatusCode(ModuleId.Player) {

  /**
   * 角色不存在
   */
  val PlayerNotExists = code(1)

  /**
   * 名字不可用
   */
  val PlayerNameNotAvailable = code(2)
}
