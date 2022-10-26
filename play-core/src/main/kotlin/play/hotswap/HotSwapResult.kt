package play.hotswap

/**
 *
 * @author LiangZengle
 */
@Suppress("MemberVisibilityCanBePrivate")
class HotSwapResult(val redefinedClasses: List<String>, val definedClasses: List<Class<*>>) {

  override fun toString(): String {
    val b = StringBuilder()
    for (name in redefinedClasses) {
      if (b.isNotEmpty()) b.appendLine()
      b.append("Redefine Class: ").append(name)
    }
    for (clazz in definedClasses) {
      if (b.isNotEmpty()) b.appendLine()
      b.append("Add Class: ").append(clazz.name)
    }
    if (b.isEmpty()) {
      return "HotSwapResult<empty>"
    }
    return b.toString()
  }
}
