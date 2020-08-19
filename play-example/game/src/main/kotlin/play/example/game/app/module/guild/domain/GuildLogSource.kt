package play.example.game.app.module.guild.domain

import play.example.common.LogSource
import play.example.common.ModularCode
import play.example.game.app.module.ModuleId

/**
 * 工会日志源
 */
@Suppress("MayBeConstant")
@ModularCode
public object GuildLogSource : LogSource(ModuleId.Guild) {

  /**
   * 创建工会
   */
  val CreateGuild = 1
}
