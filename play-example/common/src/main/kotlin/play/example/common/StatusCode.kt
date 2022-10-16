package play.example.common

import play.util.control.Result2
import play.util.control.err
import play.util.control.ok

/**
 * 公共错误码
 * @property moduleId 模块id
 * @constructor
 */
@Suppress("unused", "PropertyName")
abstract class StatusCode(val moduleId: Short) {

  /**
   * 操作成功
   */
  val Success = ok<Nothing>()

  /**
   * 操作失败
   */
  val Failure = err(1)

  /**
   * 参数错误
   */
  val ParamErr = err(2)

  /**
   * 配置不存在
   */
  val ResourceNotFound = err(3)

  /**
   * 没有奖励可领取
   */
  val NoReward = err(4)

  /**
   * 奖励已领取
   */
  val RewardReceived = err(5)

  protected fun code(number: Int): Result2<Nothing> {
    require(number in 0..999) { "`number` out of bound(0~999): $number" }
    return err(moduleId * 1000 + number)
  }

  companion object : StatusCode(0)
}
