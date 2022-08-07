package play.example.game.app.module.guild

import org.springframework.stereotype.Component
import play.example.game.app.module.ModuleId
import play.example.module.guild.message.GuildProto
import play.mvc.*

/**
 * 工会模块请求处理
 */
@GeneratePlayerRequestMessage(GuildManager.Command::class)
@Component
@Controller(ModuleId.Guild)
public class GuildController(
  private val guildService: GuildService
) : AbstractController(ModuleId.Guild) {

  @Cmd(1, dummy = true)
  fun create(playerId: GuildManager.Commander, guildName: String): RequestResult<GuildProto> =
    throw UnsupportedOperationException()

  @Cmd(2, dummy = true)
  fun join(playerId: GuildManager.Commander, guildId: Long): RequestResult<GuildProto> =
    throw UnsupportedOperationException()
}
