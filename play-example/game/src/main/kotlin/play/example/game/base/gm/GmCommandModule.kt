package play.example.game.base.gm

abstract class GmCommandModule {
  /**
   * 模块名
   */
  abstract val name: String

  protected fun ok(message: String = "操作成功") = GmCommandResult.ok(message)

  protected fun err(message: String = "操作失败") = GmCommandResult.err(message)

  /**
   * 将方法标记为gm指令
   *
   * @property name 指令名称（默认取方法名）
   * @property desc 指令描述
   * @constructor
   */
  @Retention(AnnotationRetention.RUNTIME)
  @Target(AnnotationTarget.FUNCTION)
  annotation class Cmd(val name: String = "", val desc: String)

  /**
   * 指令参数
   *
   * @property desc 参数描述
   * @property defaultValue 参数默认值
   * @constructor
   */
  @Retention(AnnotationRetention.RUNTIME)
  @Target(AnnotationTarget.VALUE_PARAMETER)
  annotation class Arg(val desc: String, val defaultValue: String = "")
}
