package play.example.module.account.controller

import play.example.module.ModuleId
import play.example.module.common.message.BoolValue
import play.mvc.AbstractController
import play.mvc.Cmd
import play.mvc.Controller
import play.mvc.RequestResult
import javax.inject.Singleton

/**
 * Created by liang on 2020/6/27.
 */
@Singleton
@Controller(ModuleId.Account)
class AccountController : AbstractController(ModuleId.Account) {

  @Cmd(1, true)
  fun login(bytes: ByteArray): RequestResult<BoolValue> {
    throw UnsupportedOperationException()
  }

  @Cmd(2, true)
  fun ping(msg: String): RequestResult<Int> {
    throw UnsupportedOperationException()
  }
}
