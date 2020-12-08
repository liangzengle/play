package play.example.module

import play.util.control.Result2
import play.util.control.err

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
  val Success = code(0)

  /**
   * 操作失败
   */
  val Failure = code(1)

  /**
   * 参数错误
   */
  val ParamErr = code(2)

  /**
   * 配置不存在
   */
  val ConfigNotFound = code(3)

  protected fun code(number: Int): Result2<Nothing> {
    require(number in 0..999) { "`number` out of bound(0~999): $number" }
    return err(moduleId * 1000 + number)
  }

  companion object : StatusCode(0)
}
