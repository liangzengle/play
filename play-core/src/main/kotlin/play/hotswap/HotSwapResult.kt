package play.hotswap

/**
 *
 * @author LiangZengle
 */
@Suppress("MemberVisibilityCanBePrivate")
class HotSwapResult(val redefinedClasses: List<String>, val definedClasses: List<String>) {

  override fun toString(): String {
    val b = StringBuilder()
    for (name in redefinedClasses) {
      if (b.isNotEmpty()) b.appendLine()
      b.append("Redefine Class: ").append(name)
    }
    for (name in definedClasses) {
      if (b.isNotEmpty()) b.appendLine()
      b.append("Add Class: ").append(name).appendLine()
    }
    if (b.isEmpty()) {
      return "HotSwapResult<empty>"
    }
    return b.toString()
  }
}
