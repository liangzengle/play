package play.util

/**
 * Formatter message patterns which arg‘s placeholder like {0}, i.e. xxx{0}xxx{1}xx
 *
 * @author LiangZengle
 */
object MessageFormatter {

  private const val UNDEFINED = "undefined"

  @JvmStatic
  fun format(pattern: String, vararg args: Any?): String {
    var appendIdx = 0
    var cursor = 0
    var b: StringBuilder? = null
    outer@
    while (cursor < pattern.length) {
      val leftBraceIdx = pattern.indexOf('{', cursor)
      if (leftBraceIdx == -1) {
        break
      }
      var rightBraceIdx = -1
      var argIdx = -1
      for (i in (leftBraceIdx + 1) ..< pattern.length) {
        val c = pattern[i]
        if (Character.isDigit(c)) {
          if (argIdx == -1) argIdx = 0
          argIdx = argIdx * 10 + (c - '0')
        } else if (c == '}' && argIdx != -1) {
          // found
          rightBraceIdx = i
          break
        } else {
          // find next valid placeholder
          cursor = if (c == '{') i else i + 1
          continue@outer
        }
      }
      if (argIdx == -1) {
        cursor++
        continue
      }
      var arg = UNDEFINED
      if (argIdx >= 0 && argIdx < args.size) {
        arg = args[argIdx]?.toString() ?: UNDEFINED
      }
      if (b == null) {
        b = StringBuilder(pattern.length + args.size * 4)
      }
      if (leftBraceIdx > appendIdx) {
        b.append(pattern, appendIdx, leftBraceIdx)
      }
      b.append(arg)
      cursor = rightBraceIdx + 1
      appendIdx = cursor
    }
    if (appendIdx == 0) {
      return pattern
    }
    if (appendIdx < pattern.length && b != null) {
      b.append(pattern, appendIdx, pattern.length)
    }
    return b.toString()
  }
}
