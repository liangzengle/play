package play.example.game.module.player.controller

import javax.inject.Inject
import javax.inject.Singleton
import play.example.game.base.gm.GmCommandService
import play.example.game.base.gm.GmResult
import play.example.game.module.ModuleId
import play.example.game.module.player.PlayerService
import play.example.game.module.player.Self
import play.example.game.module.player.message.PlayerDTO
import play.mvc.*
import play.util.control.ok

/**
 *
 * @author LiangZengle
 */
@Controller(ModuleId.Player)
@Singleton
class PlayerController @Inject constructor(
  private val service: PlayerService,
  private val gmCommandService: GmCommandService
) : AbstractController(ModuleId.Player) {

  @Cmd(1, dummy = true)
  fun create(name: String): RequestResult<Boolean> = throw UnsupportedOperationException()

  @Cmd(2, dummy = true)
  fun login(name: String): RequestResult<PlayerDTO> = throw UnsupportedOperationException()

  @Cmd(3)
  fun ping(self: Self, msg: String): RequestResult<String> = RequestResult {
    ok("$msg world!")
  }

  @Cmd(99)
  fun gm(self: Self, cmd: String): RequestResult<GmResult> = RequestResult {
    val cmdParts = cmd.split(' ')
    ok(
      if (cmdParts.size < 2) {
        GmResult(false, "GM指令参数错误: $cmd")
      } else {
        val module = cmdParts[0]
        val command = cmdParts[1]
        val args = if (cmdParts.size > 2) cmdParts.subList(2, cmdParts.size) else emptyList()
        val gmResult = gmCommandService.invoke(self, module, command, args)
        GmResult(gmResult.success, gmResult.message)
      }
    )
  }

  /**
   * 推送消息
   */
  @Cmd(101)
  lateinit var StringMessage: Push<String>
}
