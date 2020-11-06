package play.example.module.guild.domain

import play.example.module.ModularCode
import play.example.module.ModuleId
import play.example.module.StatusCode

/**
 * 工会错误码
 */
@ModularCode
public object GuildErrorCode : StatusCode(ModuleId.Guild)
