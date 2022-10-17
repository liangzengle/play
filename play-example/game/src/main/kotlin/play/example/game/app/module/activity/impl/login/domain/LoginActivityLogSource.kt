package play.example.game.app.module.activity.impl.login.domain

import play.example.common.LogSource
import play.example.common.ModularCode
import play.example.game.app.module.ModuleId

/**
 *
 * @author LiangZengle
 */
@ModularCode
object LoginActivityLogSource : LogSource(ModuleId.ActivityLogin) {

  /** 登录活动奖励 */
  val Reward = 1
}
