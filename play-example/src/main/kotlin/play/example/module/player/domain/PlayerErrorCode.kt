package play.example.module.player.domain

import play.example.module.ModularCode
import play.example.module.ModuleId
import play.example.module.StatusCode

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
