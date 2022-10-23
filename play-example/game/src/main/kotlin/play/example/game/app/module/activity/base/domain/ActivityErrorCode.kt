package play.example.game.app.module.activity.base.domain

import play.example.common.StatusCode
import play.example.game.app.module.ModuleId

/**
 *
 * @author LiangZengle
 */
object ActivityErrorCode : StatusCode(ModuleId.Activity) {

  /**
   * 活动已关闭
   */
  val ActivityClosed = code(1)
}
