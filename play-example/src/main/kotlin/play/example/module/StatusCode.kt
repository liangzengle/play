package play.example.module

/**
 * 公共错误码
 * @author LiangZengle
 */
@Suppress("unused", "PropertyName")
abstract class StatusCode(val moduleId: Short) {
  // 操作成功
  val Success = 0

  // 操作失败
  val Failure = -1

  // 参数错误
  val ParamErr = -2

  companion object : StatusCode(0)
}
