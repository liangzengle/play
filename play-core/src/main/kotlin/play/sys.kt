package play

import java.io.File
import play.SystemProps.osName

object sys {

  init {
    SystemProps.setIfAbsent("availableProcessors", runtime().availableProcessors())
  }

  @JvmStatic
  fun runtime(): Runtime {
    return Runtime.getRuntime()
  }

  @JvmStatic
  fun availableProcessors(): Int {
    return SystemProps.getInt("availableProcessors")
  }

  @JvmStatic
  val isWindows: Boolean
    get() = isOS("Windows")

  @JvmStatic
  val isMac: Boolean
    get() = isOS("Mac OS")

  @JvmStatic
  val isLinux: Boolean
    get() = isOS("Linux")

  private fun isOS(osName: String): Boolean {
    return osName().startsWith(osName)
  }

  @JvmStatic
  fun fileSeparator(): String {
    return File.separator
  }

  @JvmStatic
  fun pathSeparator(): String {
    return File.pathSeparator
  }

  @JvmStatic
  fun lineSeparator(): String {
    return System.lineSeparator()
  }
}
