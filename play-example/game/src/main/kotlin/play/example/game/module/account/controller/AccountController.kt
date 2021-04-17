package play.example.game.module.account.controller

import javax.inject.Singleton
import play.example.game.module.ModuleId
import play.mvc.AbstractController
import play.mvc.Cmd
import play.mvc.Controller
import play.mvc.RequestResult

/**
 * Created by liang on 2020/6/27.
 */
@Singleton
@Controller(ModuleId.Account)
class AccountController : AbstractController(ModuleId.Account) {

  @Cmd(1, true)
  fun login(bytes: ByteArray): RequestResult<Boolean> {
    throw UnsupportedOperationException()
  }

  @Cmd(2, true)
  fun ping(msg: String): RequestResult<Int> {
    throw UnsupportedOperationException()
  }
}
