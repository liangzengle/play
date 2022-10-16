package play.example.game.app.module.activity.impl.login.domain

import play.example.common.ModularCode
import play.example.common.StatusCode
import play.example.game.app.module.ModuleId

/**
 *
 * @author LiangZengle
 */
@ModularCode
object LoginActivityStatusCode : StatusCode(ModuleId.ActivityLogin) {

  /**
   * 登录天数不足
   */
  val LoginNotEnough = code(1)
}
