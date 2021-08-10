package play

import java.io.File

object OS {

  @JvmStatic
  fun osName(): String {
    return SystemProps.getOrEmpty("os.name")
  }

  @JvmStatic
  val isWindows: Boolean
    get() = isOS("Windows")

  @JvmStatic
  val isMac: Boolean
    get() = isOS("Mac")

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
