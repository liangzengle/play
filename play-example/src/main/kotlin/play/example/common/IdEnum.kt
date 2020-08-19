package play.example.common

import play.util.collection.filterDuplicatedBy
import play.util.collection.toImmutableMap

/**
 * Created by liang on 2020/6/27.
 */
interface IdEnum<T : Enum<T>> {
  val id: Int
}

interface IdEnumCompanion<T> where T : Enum<T>, T : IdEnum<T> {

  val elems: Array<T>

  fun getOrNull(id: Int): T? {
    for (elem in elems) {
      if (id == elem.id) {
        return elem
      }
    }
    return null
  }

  fun getOrThrow(id: Int): T {
    return getOrNull(id) ?: throw NoSuchElementException(id.toString())
  }

  fun getOrDefault(id: Int, default: T): T {
    return getOrNull(id) ?: default
  }

  @Suppress("JAVA_CLASS_ON_COMPANION", "NOTHING_TO_INLINE", "UNCHECKED_CAST")
  fun ensureUniqueId() {
    ensureUniqueId(elems as Array<IdEnum<*>>)
  }

  private fun ensureUniqueId(elems: Array<IdEnum<*>>) {
    val duplicated = elems.asSequence()
      .filterDuplicatedBy { it.id }
      .asSequence()
      .filter { it.value.size > 1 }
      .toList()
    if (duplicated.isNotEmpty()) {
      throw IllegalStateException("枚举id重复: $duplicated")
    }
  }

  fun toIdMap(): Map<Int, T> = elems.toImmutableMap { it.id }
}

