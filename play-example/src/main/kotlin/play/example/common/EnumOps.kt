package play.example.common

import com.google.common.collect.Maps
import org.eclipse.collections.api.map.primitive.IntObjectMap
import org.eclipse.collections.impl.factory.primitive.IntObjectMaps
import play.Log
import play.util.collection.filterDuplicatedBy

interface IdEnumOps<T> where T : Enum<T>, T : IdEnum<T> {

  val elems: List<T>

  val size: Int

  fun getOrNull(id: Int): T?

  fun getOrThrow(id: Int): T

  fun getOrDefault(id: Int, default: T): T

  fun list(): List<T> = elems
}

interface NamedEnumOps<T> where T : Enum<T>, T : NamedEnum<T> {

  val elems: List<T>

  val size: Int

  fun getOrNull(name: String): T?

  fun getOrThrow(name: String): T

  fun getOrDefault(name: String, default: T): T

  fun getIgnoreCaseOrNull(name: String): T?
}

interface IdNamedEnumOps<T> : IdEnumOps<T>, NamedEnumOps<T> where T : Enum<T>, T : IdEnum<T>, T : NamedEnum<T>

private class IdEnumOpsImpl<T>(override val elems: List<T>) : IdEnumOps<T> where T : Enum<T>, T : IdEnum<T> {

  init {
    val duplicated = elems.asSequence().filterDuplicatedBy { it.id }.values
    if (duplicated.isNotEmpty()) {
      val msg = "${elems[0].javaClass.simpleName}存在重复的id: $duplicated"
      Log.error { msg }
      throw IllegalStateException(msg)
    }
  }

  private val map: IntObjectMap<T>? =
    if (elems.size > 8) IntObjectMaps.immutable.from(elems, { it.id }, { it }) else null

  override val size: Int get() = elems.size

  override fun getOrNull(id: Int): T? {
    if (map != null) return map.get(id)
    for (i in elems.indices) {
      val elem = elems[i]
      if (id == elem.id) {
        return elem
      }
    }
    return null
  }

  override fun getOrThrow(id: Int): T {
    return getOrNull(id) ?: throw NoSuchElementException(id.toString())
  }

  override fun getOrDefault(id: Int, default: T): T {
    return getOrNull(id) ?: default
  }
}

private class NamedEnumOpsImpl<T>(override val elems: List<T>) : NamedEnumOps<T> where T : Enum<T>, T : NamedEnum<T> {
  init {
    val duplicated = elems.asSequence().filterDuplicatedBy { it.getName() }.values
    if (duplicated.isNotEmpty()) {
      val msg = "${elems[0].javaClass.simpleName}存在重复的name: $duplicated"
      Log.error { msg }
      throw IllegalStateException(msg)
    }
  }

  private val map: Map<String, T>? = if (elems.size > 8) Maps.uniqueIndex(elems) { it!!.getName() } else null

  override val size: Int get() = elems.size

  override fun getOrNull(name: String): T? {
    if (map != null) {
      return map[name]
    }
    for (i in elems.indices) {
      val elem = elems[i]
      if (name == elem.getName()) {
        return elem
      }
    }
    return null
  }

  override fun getOrThrow(name: String): T {
    return getOrNull(name) ?: throw NoSuchElementException(name)
  }

  override fun getOrDefault(name: String, default: T): T {
    return getOrNull(name) ?: default
  }

  override fun getIgnoreCaseOrNull(name: String): T? {
    for (i in elems.indices) {
      val elem = elems[i]
      if (name.equals(elem.getName(), true)) {
        return elem
      }
    }
    return null
  }
}

private class IdNamedEnumOpsImpl<T>(elems: List<T>) :
  IdNamedEnumOps<T> where T : Enum<T>, T : IdEnum<T>, T : NamedEnum<T> {
  private val idOps = IdEnumOpsImpl(elems)
  private val namedOps = NamedEnumOpsImpl(elems)

  override val elems: List<T> = idOps.elems

  override val size: Int get() = idOps.size

  override fun getOrNull(id: Int): T? = idOps.getOrNull(id)

  override fun getOrThrow(id: Int): T = idOps.getOrThrow(id)

  override fun getOrDefault(id: Int, default: T): T = idOps.getOrDefault(id, default)

  override fun getOrNull(name: String): T? = namedOps.getOrNull(name)

  override fun getOrThrow(name: String): T = namedOps.getOrThrow(name)

  override fun getOrDefault(name: String, default: T): T =
    namedOps.getOrDefault(name, default)

  override fun getIgnoreCaseOrNull(name: String): T? = namedOps.getIgnoreCaseOrNull(name)
}

fun <T> idEnumOpsOf(elems: Array<T>): IdEnumOps<T> where T : Enum<T>, T : IdEnum<T> = IdEnumOpsImpl(elems.asList())

fun <T> namedEnumOpsOf(elems: Array<T>): NamedEnumOps<T> where T : Enum<T>, T : NamedEnum<T> =
  NamedEnumOpsImpl(elems.asList())

fun <T> idNamedEnumOpsOf(elems: Array<T>): IdNamedEnumOps<T> where T : Enum<T>, T : IdEnum<T>, T : NamedEnum<T> =
  IdNamedEnumOpsImpl(elems.asList())
