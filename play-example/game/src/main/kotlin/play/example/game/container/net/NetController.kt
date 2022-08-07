package play.example.game.container.net

import org.springframework.stereotype.Component
import play.example.game.app.module.ModuleId
import play.mvc.*

/**
 *
 *
 * @author LiangZengle
 */
@Controller(ModuleId.Net)
@Component
class NetController : AbstractController(ModuleId.Net) {

  @Cmd(1)
  fun heartbeat(clientTime: Long): RequestResult<Long> {
    return RequestResult.ok(1L)
  }

  @Cmd(2)
  lateinit var sessionClose: Push<Int>
}
