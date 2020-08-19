package play.example.module.player.controller

import play.example.common.net.message.ok
import play.example.module.ModuleId
import play.example.module.common.message.BoolValue
import play.example.module.common.message.StringValue
import play.example.module.player.PlayerService
import play.example.module.player.message.PlayerProto
import play.mvc.AbstractController
import play.mvc.Cmd
import play.mvc.Controller
import play.mvc.RequestResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 *
 * @author LiangZengle
 */
@Controller(ModuleId.Player)
@Singleton
class PlayerController @Inject constructor(private val service: PlayerService) : AbstractController(ModuleId.Player) {

  @Cmd(1, dummy = true)
  fun create(name: String): RequestResult<BoolValue> = throw UnsupportedOperationException()

  @Cmd(2, dummy = true)
  fun login(name: String): RequestResult<PlayerProto> = throw UnsupportedOperationException()

  @Cmd(3)
  fun ping(msg: String): RequestResult<StringValue> = RequestResult.ok {
    StringValue("$msg world!")
  }
}
