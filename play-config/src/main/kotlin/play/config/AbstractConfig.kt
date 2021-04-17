package play.config

/**
 * 所有配置的基类
 */
abstract class AbstractConfig constructor(@JvmField val id: Int) {

  // for deserialization framework
  constructor() : this(0)

  open fun postInitialize(configSetSupplier: ConfigSetSupplier, errors: MutableCollection<String>) {}

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as AbstractConfig

    if (id != other.id) return false

    return true
  }

  override fun hashCode(): Int {
    return id.hashCode()
  }

  override fun toString(): String {
    return "${javaClass.simpleName}($id)"
  }
}

interface UniqueKey<K> {
  fun key(): K
  fun keyComparator(): Comparator<K>
}

interface ComparableUniqueKey<K : Comparable<K>> : UniqueKey<K> {
  override fun keyComparator(): Comparator<K> = Comparator.naturalOrder()
}

interface Grouped<G> {
  fun groupId(): G
}

interface ExtensionKey<E : ConfigExtension<out AbstractConfig>>

@Suppress("UNUSED_PARAMETER")
abstract class ConfigExtension<T : AbstractConfig>(elems: List<T>)
