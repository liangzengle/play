package play.example.game.app.module.player

import org.springframework.stereotype.Component
import play.example.game.app.module.ModuleId
import play.example.game.app.module.player.message.PlayerDTO
import play.example.game.container.gm.GmCommandService
import play.example.game.container.gm.GmResult
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
  private val gmCommandService: GmCommandService<Self>
) : AbstractController(ModuleId.Player) {

  @Cmd(1, dummy = true)
  fun create(name: String): RequestResult<Boolean> = throw UnsupportedOperationException()

  @Cmd(2, dummy = true)
  fun login(): RequestResult<PlayerDTO> = throw UnsupportedOperationException()

  @Cmd(3)
  fun ping(self: Self, msg: String): RequestResult<String> = RequestResult {
    ok("world!")
  }

  @Cmd(99)
  fun gm(self: Self, cmd: String): RequestResult<GmResult> = RequestResult {
    val cmdParts = cmd.split(' ')
    ok {
      if (cmdParts.size < 2) {
        GmResult(false, "GM指令参数错误: $cmd")
      } else {
        val module = cmdParts[0]
        val command = cmdParts[1]
        val args = if (cmdParts.size > 2) cmdParts.subList(2, cmdParts.size) else emptyList()
        val gmResult = gmCommandService.invoke(self, module, command, args)
        GmResult(gmResult.success, gmResult.message)
      }
    }
  }

  /**
   * 推送消息
   */
  @Cmd(101)
  lateinit var StringMessage: Push<String>
}
