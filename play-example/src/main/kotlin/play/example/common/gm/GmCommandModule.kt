package play.example.common.gm

abstract class GmCommandModule {
  /**
   * 模块名
   */
  abstract val name: String

  protected fun ok(message: String = "操作成功") = GmCommandResult.ok(message)

  protected fun err(message: String = "操作失败") = GmCommandResult.err(message)
}
