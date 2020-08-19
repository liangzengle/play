package play.example.module.account.domain

import play.example.module.ModularCode
import play.example.module.ModuleId
import play.example.module.StatusCode

/**
 * 账号相关错误码
 * @author LiangZengle
 */
@ModularCode
object AccountErrorCode : StatusCode(ModuleId.Account) {

  // 平台不支持
  val NoSuchPlatform = 1

  // 平台非法
  val InvalidPlatform = 2

  // 服id非法
  val InvalidServerId = 3

  // 服务器人员已满，无法注册新账号
  val IdExhausted = 4;
}
