package play.util.enumration

import org.eclipse.collections.api.factory.Maps
import org.eclipse.collections.impl.factory.primitive.IntObjectMaps
import play.util.unsafeCast

/**
 * 运行时枚举
 * @author LiangZengle
 */
abstract class RuntimeEnum(@JvmField val id: Int, @JvmField val name: String, @JvmField val desc: String) {

  override fun toString(): String {
    return "${javaClass.simpleName}(id=$id, name='$name', desc='$desc')"
  }

  @Suppress("TYPE_INFERENCE_ONLY_INPUT_TYPES_WARNING")
  companion object {
    @JvmStatic
    private var factories = Maps.immutable.empty<Class<out RuntimeEnum>, Factory<out RuntimeEnum>>()

    @JvmStatic
    fun <T> values(type: Class<T>): Collection<T> = factories[type]!!.values().unsafeCast()

    fun <T> getOrThrow(type: Class<T>, id: Int): Collection<T> = factories[type]!!.getOrThrow(id).unsafeCast()
    fun <T> getOrThrow(type: Class<T>, name: String): Collection<T> = factories[type]!!.getOrThrow(name).unsafeCast()
    fun <T> getOrNull(type: Class<T>, id: Int): Collection<T> = factories[type]!!.getOrNull(id).unsafeCast()
    fun <T> getOrNull(type: Class<T>, name: String): Collection<T> = factories[type]!!.getOrNull(name).unsafeCast()
  }

  class Factory<T : RuntimeEnum>(enumType: Class<T>) : RuntimeEnumFactoryOps<T> {
    init {
      synchronized(Companion) {
        factories.newWithKeyValue(enumType.unsafeCast(), this.unsafeCast())
      }
    }

    private var idMap = IntObjectMaps.immutable.empty<T>()
    private var nameMap = Maps.immutable.empty<String, T>()

    @Synchronized
    override fun create(t: T) {
      if (idMap[t.id]?.let { it !== t } == true) {
        throw IllegalStateException("Duplicated id: ${t.id}")
      }
      if (nameMap[t.name]?.let { it !== t } == true) {
        throw IllegalStateException("Duplicated name: ${t.name}")
      }
      idMap = idMap.newWithKeyValue(t.id, t)
      nameMap = nameMap.newWithKeyValue(t.name, t)
    }

    override fun values(): Collection<T> = idMap.values()

    override fun getOrThrow(id: Int): T = idMap[id] ?: throw NoSuchElementException(id.toString())
    override fun getOrThrow(name: String): T = nameMap[name] ?: throw NoSuchElementException(name)

    override fun getOrNull(id: Int): T = idMap[id]
    override fun getOrNull(name: String): T = nameMap[name]
  }
}

interface RuntimeEnumFactoryOps<T : RuntimeEnum> {
  fun create(t: T)

  fun values(): Collection<T>

  @Throws(NoSuchElementException::class)
  fun getOrThrow(id: Int): T

  @Throws(NoSuchElementException::class)
  fun getOrThrow(name: String): T

  fun getOrNull(id: Int): T
  fun getOrNull(name: String): T
}
