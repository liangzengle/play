package play.example.game.app.module.guild

import play.example.game.app.module.ModuleId
import play.example.game.app.module.guild.message.GuildInfo
import play.mvc.*
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * 工会模块请求处理
 */
@GeneratePlayerRequestMessage(GuildManager.Command::class)
@Singleton
@Named
@Controller(ModuleId.Guild)
public class GuildController @Inject constructor(
  private val guildService: GuildService
) : AbstractController(ModuleId.Guild) {

  @Cmd(1, dummy = true)
  fun create(playerId: Long, guildName: String): RequestResult<GuildInfo> = throw UnsupportedOperationException()

  @Cmd(2, dummy = true)
  fun join(playerId: Long, guildId: Long): RequestResult<GuildInfo> = throw UnsupportedOperationException()
}
