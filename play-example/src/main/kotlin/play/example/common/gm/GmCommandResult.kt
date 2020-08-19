package play.example.common.gm

class GmCommandResult private constructor(val success: Boolean, val message: String) {

  companion object {
    fun ok(message: String = "操作成功") = GmCommandResult(true, message)
    fun err(message: String = "操作失败") = GmCommandResult(true, message)
  }
}
