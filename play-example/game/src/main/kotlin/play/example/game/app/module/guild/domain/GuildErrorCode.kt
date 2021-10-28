package play.example.game.app.module.guild.domain

import play.example.common.ModularCode
import play.example.common.StatusCode
import play.example.game.app.module.ModuleId

/**
 * 工会错误码
 */
@Suppress("MayBeConstant")
@ModularCode
public object GuildErrorCode : StatusCode(ModuleId.Guild) {

  /**
   * 已经在工会中
   */
  val HasGuild: Int = 1

  /**
   * 工会名称不可用
   */
  val NameNotAvailable: Int = 2

  /**
   * 工会不存在
   */
  val GuildNotExists: Int = 3
}
