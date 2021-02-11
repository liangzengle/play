package play.example.common

import org.eclipse.collections.impl.factory.Maps
import org.eclipse.collections.impl.factory.primitive.IntObjectMaps
import play.util.reflect.Reflect
import play.util.unsafeCast

/**
 * 运行时枚举
 * @author LiangZengle
 */
@Suppress("LeakingThis")
abstract class RuntimeEnum(@JvmField val id: Int, @JvmField val name: String, @JvmField val desc: String) {
  constructor(id: Int, name: String) : this(id, name, name)

  init {
    Factory(this.javaClass).intern(this)
  }

  override fun toString(): String {
    return "${javaClass.simpleName}(id=$id, name='$name')"
  }

  companion object {
    @JvmStatic
    private var factories = Maps.immutable.empty<Class<out RuntimeEnum>, Factory<out RuntimeEnum>>()

    @JvmStatic
    fun <T : RuntimeEnum> values(type: Class<T>): Collection<T> = factories[type]!!.values().unsafeCast()

    @JvmStatic
    fun <T : RuntimeEnum> getOrThrow(type: Class<T>, id: Int): Collection<T> =
      factories[type]!!.getOrThrow(id).unsafeCast()

    @JvmStatic
    fun <T : RuntimeEnum> getOrThrow(type: Class<T>, name: String): Collection<T> =
      factories[type]!!.getOrThrow(name).unsafeCast()

    @JvmStatic
    fun <T : RuntimeEnum> getOrNull(type: Class<T>, id: Int): T? = factories[type]!!.getOrNull(id)?.unsafeCast()

    @JvmStatic
    fun <T : RuntimeEnum> getOrNull(type: Class<T>, name: String): T? = factories[type]!!.getOrNull(name)?.unsafeCast()

    @JvmStatic
    fun <T : RuntimeEnum> getOrDefault(name: String, default: T): T =
      factories[default.javaClass]!!.unsafeCast<Factory<T>>().getOrDefault(name, default)

    @JvmStatic
    fun <T : RuntimeEnum> getOrDefault(id: Int, default: T): T =
      factories[default.javaClass]!!.unsafeCast<Factory<T>>().getOrDefault(id, default)
  }

  class Factory<T : RuntimeEnum> private constructor() : RuntimeEnumFactoryOps<T> {

    companion object {
      @JvmName("get")
      @JvmStatic
      @Synchronized
      operator fun <T : RuntimeEnum> invoke(type: Class<T>): Factory<T> {
        var factory = factories[type]
        if (factory == null) {
          factory = Factory<T>()
          factories = factories.newWithKeyValue(type, factory)
        }
        return factory.unsafeCast()
      }
    }

    private var idMap = IntObjectMaps.immutable.empty<T>()
    private var nameMap = Maps.immutable.empty<String, T>()

    @Synchronized
    override fun intern(t: T) {
      if (idMap[t.id] != null) {
        throw IllegalStateException("Duplicated id: ${t.id}")
      }
      if (nameMap[t.name] != null) {
        throw IllegalStateException("Duplicated name: ${t.name}")
      }
      idMap = idMap.newWithKeyValue(t.id, t)
      nameMap = nameMap.newWithKeyValue(t.name, t)
    }

    override fun values(): Collection<T> = idMap.values()

    override fun getOrThrow(id: Int): T = idMap[id] ?: throw NoSuchElementException(id.toString())
    override fun getOrThrow(name: String): T = nameMap[name] ?: throw NoSuchElementException(name)

    override fun getOrNull(id: Int): T? = idMap[id]
    override fun getOrNull(name: String): T? = nameMap[name]
  }
}

interface RuntimeEnumFactoryOps<T : RuntimeEnum> {
  fun intern(t: T)

  fun values(): Collection<T>

  @Throws(NoSuchElementException::class)
  fun getOrThrow(id: Int): T

  @Throws(NoSuchElementException::class)
  fun getOrThrow(name: String): T

  fun getOrNull(id: Int): T?
  fun getOrNull(name: String): T?

  fun getOrDefault(id: Int, default: T): T = getOrNull(id) ?: default
  fun getOrDefault(name: String, default: T): T = getOrNull(name) ?: default
}


fun main() {
  val o = object : RuntimeEnum(1, "") {

  }

  println(Reflect.isTopLevelClass(RuntimeEnum::class.java))
  println(Reflect.isTopLevelClass(RuntimeEnum.Companion::class.java))

  println(Reflect.isTopLevelClass(RuntimeEnum.Companion::class.java))
}
