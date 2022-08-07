package play.example.game.container.command

class CommandResult private constructor(val success: Boolean, val message: String) {

  override fun toString(): String {
    return if (success) "Success($message)" else "Failure($message)"
  }

  companion object {
    @JvmStatic
    fun ok(message: String = "操作成功") = CommandResult(true, message)

    @JvmStatic
    fun err(message: String = "操作失败") = CommandResult(false, message)
  }
}
