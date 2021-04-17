package play.example.game.module.guild

import javax.inject.Inject
import javax.inject.Singleton
import play.example.game.module.ModuleId
import play.example.game.module.guild.message.GuildInfo
import play.mvc.*

/**
 * 工会模块请求处理
 */
@GeneratePlayerRequestMessage(GuildManager.Command::class)
@Singleton
@Controller(ModuleId.Guild)
public class GuildController @Inject constructor(
  private val guildService: GuildService
) : AbstractController(ModuleId.Guild) {

  @Cmd(1, dummy = true)
  fun create(playerId: Long, guildName: String): RequestResult<GuildInfo> = throw UnsupportedOperationException()

  @Cmd(2, dummy = true)
  fun join(playerId: Long, guildId: Long): RequestResult<GuildInfo> = throw UnsupportedOperationException()
}
