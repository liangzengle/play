package play.example.module.guild

import javax.inject.Inject
import javax.inject.Singleton
import play.example.module.ModuleId
import play.example.module.guild.GuildManager
import play.example.module.guild.message.GuildInfo
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

  @Cmd(1)
  fun create(playerId: Long, guildName: String): RequestResult<GuildInfo> = throw UnsupportedOperationException()

  @Cmd(2)
  fun join(playerId: Long, guildId: Long): RequestResult<GuildInfo> = throw UnsupportedOperationException()
}
