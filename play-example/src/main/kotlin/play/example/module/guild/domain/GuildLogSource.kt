package play.example.module.guild.domain

import play.example.module.LogSource
import play.example.module.ModularCode
import play.example.module.ModuleId

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
