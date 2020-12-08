package play

/**
 * 不同模式下不同的行为
 *
 * @author LiangZengle
 */
object ModeDependent {
  @JvmStatic
  lateinit var mode: Mode
    private set

  fun setMode(mode: Mode) {
    this.mode = mode
    Log.info { "ModeDependent: $mode" }
  }

  /**
   * 记录日志并根据当前的[mode]选择抛异常还是使用默认值
   *
   * @param e 发生的异常
   * @param defaultValue 默认值
   * @param message 错误信息
   * @return 默认值 或者 抛异常
   */
  @JvmStatic
  fun <T> logAndRecover(e: Throwable, defaultValue: T, message: () -> Any?): T {
    Log.error(e, message)
    return if (!mode.isDev()) throw e else defaultValue
  }
}
