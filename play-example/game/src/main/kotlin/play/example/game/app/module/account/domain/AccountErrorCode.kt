package play.example.game.app.module.account.domain

import play.example.common.ModularCode
import play.example.common.StatusCode
import play.example.game.app.module.ModuleId

/**
 * 账号相关错误码
 * @author LiangZengle
 */
@Suppress("MayBeConstant")
@ModularCode
object AccountErrorCode : StatusCode(ModuleId.Account) {

  /**
   * 平台不支持
   */
  val NoSuchPlatform = code(1)

  /**
   * 平台非法
   */
  val InvalidPlatform = code(2)

  /**
   * 服id非法
   */
  val InvalidServerId = code(3)

  /**
   * 服务器人员已满，无法注册新账号
   */
  val IdExhausted = code(4)
}
