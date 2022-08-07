package play.example.game.app.module.player

import org.springframework.stereotype.Component
import play.example.game.app.module.ModuleId
import play.example.game.container.command.CommandService
import play.example.player.message.PlayerProto
import play.mvc.*
import play.util.control.ok

/**
 *
 * @author LiangZengle
 */
@Controller(ModuleId.Player)
@Component
class PlayerController(
  private val service: PlayerService,
  private val commandService: CommandService
) : AbstractController(ModuleId.Player) {

  @Cmd(1, dummy = true)
  fun create(name: String): RequestResult<Boolean> = throw UnsupportedOperationException()

  @Cmd(2, dummy = true)
  fun login(): RequestResult<PlayerProto> = throw UnsupportedOperationException()

  @Cmd(3)
  fun ping(self: PlayerManager.Self, msg: String): RequestResult<String> = ok("world!").toRequestResult()

  /**
   * 推送消息
   */
  @Cmd(101)
  lateinit var StringMessage: Push<String>
}
