package play.example.module.guild.controller

import play.example.common.net.message.async
import play.example.module.ModuleId
import play.example.module.guild.GuildService
import play.mvc.AbstractController
import play.mvc.Cmd
import play.mvc.Controller
import play.mvc.RequestResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 工会模块请求处理
 */
@Singleton
@Controller(ModuleId.Guild)
public class GuildController @Inject constructor(
  private val guildService: GuildService
) : AbstractController(ModuleId.Guild) {


  @Cmd(1)
  fun create(playerId: Long, guildName: String) = RequestResult.async {
    guildService.createGuild(playerId, guildName)
  }
}
