package play.example.game.app.module.activity.impl.login

import org.springframework.stereotype.Component
import play.example.game.app.module.ModuleId
import play.example.game.app.module.player.PlayerManager
import play.mvc.AbstractController
import play.mvc.Cmd
import play.mvc.Controller
import play.mvc.RequestResult

/**
 *
 * @author LiangZengle
 */
@Component
@Controller(ModuleId.ActivityLogin)
class LoginActivityController(private val handler: LoginActivityHandler) : AbstractController(ModuleId.ActivityLogin) {

  /**
   * 请求领取登录奖励
   *
   * @param self Self
   * @param day 第几天的奖励
   */
  @Cmd(1)
  fun getReward(self: PlayerManager.Self, activityId: Int, day: Int) = RequestResult {
    handler.getReward(self, activityId, day)
  }
}
