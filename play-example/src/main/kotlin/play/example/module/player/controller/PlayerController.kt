package play.example.module.player.controller

import play.example.common.gm.GmCommandService
import play.example.common.net.SessionWriter
import play.example.common.net.message.of
import play.example.common.net.message.ok
import play.example.module.ModuleId
import play.example.module.common.message.BoolValue
import play.example.module.common.message.StringValue
import play.example.module.player.PlayerService
import play.example.module.player.Self
import play.example.module.player.message.GmResultProto
import play.example.module.player.message.PlayerProto
import play.mvc.*
import javax.inject.Inject
import javax.inject.Singleton

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
  fun create(name: String): RequestResult<BoolValue> = throw UnsupportedOperationException()

  @Cmd(2, dummy = true)
  fun login(name: String): RequestResult<PlayerProto> = throw UnsupportedOperationException()

  @Cmd(3)
  fun ping(self: Self, msg: String): RequestResult<StringValue> = RequestResult.ok {
    SessionWriter.write(
      self.id,
      PlayerControllerInvoker.StringMessagePush.of(StringValue("this is a pushed message"))
    )
    StringValue("$msg world!")
  }

  @Cmd(99)
  fun gm(self: Self, cmd: String): RequestResult<GmResultProto> = RequestResult.ok {
    val cmdParts = cmd.split(' ')
    if (cmdParts.size < 2) {
      GmResultProto(false, "GM指令参数错误: $cmd")
    } else {
      val module = cmdParts[0]
      val command = cmdParts[1]
      val args = if (cmdParts.size > 2) cmdParts.subList(2, cmdParts.size) else emptyList()
      val gmResult = gmCommandService.invoke(self, module, command, args)
      GmResultProto(gmResult.success, gmResult.message)
    }
  }

  /**
   * 推送消息
   */
  @Cmd(101)
  lateinit var StringMessage: Push<StringValue>
}
