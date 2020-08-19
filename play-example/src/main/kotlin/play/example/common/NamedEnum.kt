package play.example.common

import play.util.collection.filterDuplicatedBy
import play.util.collection.toImmutableMap

/**
 * Created by liang on 2020/6/27.
 */
interface NamedEnum<T : Enum<T>> {
  fun getName(): String
}

interface NamedEnumCompanion<T> where T : Enum<T>, T : NamedEnum<T> {

  val elems: Array<T>

  fun getOrNull(name: String, ignoreCase: Boolean = false): T? {
    for (elem in elems) {
      if (name == elem.getName()) {
        return elem
      }
      if (ignoreCase && name.equals(elem.getName(), true)) {
        return elem
      }
    }
    return null
  }

  fun getOrThrow(name: String, ignoreCase: Boolean = false): T {
    return getOrNull(name, ignoreCase) ?: throw NoSuchElementException("平台不存在: $name")
  }

  fun getOrDefault(name: String, default: T, ignoreCase: Boolean = false): T {
    return getOrNull(name, ignoreCase) ?: default
  }

  @Suppress("JAVA_CLASS_ON_COMPANION", "NOTHING_TO_INLINE", "UNCHECKED_CAST")
  fun ensureUniqueName() {
    ensureUniqueName(elems as Array<NamedEnum<*>>)
  }

  private fun ensureUniqueName(elems: Array<NamedEnum<*>>) {
    val duplicated = elems.asSequence()
      .filterDuplicatedBy { it.getName() }
      .asSequence().filter { it.value.size > 1 }
      .toList()
    if (duplicated.isNotEmpty()) {
      throw IllegalStateException("枚举name重复: $duplicated")
    }
  }

  fun toNameMap(): Map<String, T> = elems.toImmutableMap { it.getName() }
}
