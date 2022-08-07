package play.example.game.app.module.account

import org.springframework.stereotype.Component
import play.example.game.app.module.ModuleId
import play.example.module.login.message.LoginParams
import play.mvc.AbstractController
import play.mvc.Cmd
import play.mvc.Controller
import play.mvc.RequestResult

/**
 * Created by liang on 2020/6/27.
 */
@Component
@Controller(ModuleId.Account)
class AccountController : AbstractController(ModuleId.Account) {

  @Cmd(1, true)
  fun login(loginParams: LoginParams): RequestResult<Boolean> {
    throw UnsupportedOperationException()
  }

  @Cmd(2, true)
  fun ping(msg: String): RequestResult<String> {
    throw UnsupportedOperationException()
  }
}
