package play.res

/**
 * 所有配置的基类
 */
abstract class AbstractResource constructor(@JvmField val id: Int) {

  // for deserialization framework
  constructor() : this(0)

  /**
   * 自定义初始化
   *
   * @param resourceSetProvider 获取ResourceSet
   * @param errors 初始化时的错误信息
   */
  open fun initialize(resourceSetProvider: ResourceSetProvider, errors: MutableCollection<String>) {}

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as AbstractResource

    if (id != other.id) return false

    return true
  }

  override fun hashCode(): Int {
    return id
  }

  override fun toString(): String {
    return "${javaClass.simpleName}($id)"
  }
}

/**
 * 应用配置，支持格式: .conf .json .properties
 */
abstract class AbstractConfig : AbstractResource(1)

/**
 * 唯一的key
 */
interface UniqueKey<K : Comparable<K>> {
  fun key(): K
}

/**
 * 分组的
 */
interface Grouped<G> {
  /**
   * 组id
   */
  fun groupBy(): G
}

/**
 * 组内唯一key
 */
interface GroupedUniqueKey<G, K : Comparable<K>> : Grouped<G> {
  /**
   * 组内唯一key
   */
  fun keyInGroup(): K
}

/**
 * 指定扩展类
 */
interface ExtensionKey<E : ResourceExtension<out AbstractResource>>

/**
 * 扩展类
 *
 * @param elems 配置对象列表
 */
@Suppress("UNUSED_PARAMETER")
abstract class ResourceExtension<T : AbstractResource>(protected val elements: List<T>)
