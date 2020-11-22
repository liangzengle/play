package play.db.cache

import play.db.EntityInt
import play.util.concurrent.Future
import play.util.function.IntToObjFunction
import java.util.*
import java.util.stream.Stream
import kotlin.NoSuchElementException

/**
 *
 * @author LiangZengle
 */
interface EntityCacheInt<E : EntityInt> {

  @JvmDefault
  operator fun invoke(id: Int): E = getOrThrow(id)

  /**
   * 对应实体类
   */
  val entityClass: Class<E>

  /**
   * 从缓存中获取实体对象，如果不存在则从数据库中加载
   */
  fun get(id: Int): Optional<E>

  /**
   * 从缓存中获取实体对象，如果不存在则从数据库中加载
   */
  fun getOrNull(id: Int): E?

  /**
   * 从缓存中获取实体对象，如果不存在则从数据库中加载，如果数据中不存在则抛出异常: [NoSuchElementException]
   */
  @Throws(NoSuchElementException::class)
  fun getOrThrow(id: Int): E

  /**
   * 从缓存中获取实体对象，如果不存在则从数据库中加载，如果数据库中不存在则创建
   */
  fun getOrCreate(id: Int, creation: IntToObjFunction<E>): E

  /**
   * 从缓存中获取实体对象，不从数据库中加载
   */
  fun getCached(id: Int): Optional<E>

  /**
   * 获取缓存中所有的实体
   */
  fun asSequence(): Sequence<E>

  /**
   * 获取缓存中所有的实体
   */
  fun asStream(): Stream<E>

  /**
   * 创建实体，如果实体已经存在则抛异常: [EntityExistsException]
   */
  @Throws(EntityExistsException::class)
  fun create(e: E): E

  /**
   * 从缓存和数据库中移除实体
   */
  fun remove(e: E)

  /**
   * 从缓存和数据库中移除id对应的实体
   */
  fun removeById(id: Int)

  /**
   * 更新数据库
   */
  fun save(e: E)

  /**
   * 缓存中的实体数量
   */
  fun size(): Int

  /**
   * 对应的实体是否在缓存中
   */
  fun isCached(id: Int): Boolean

  /**
   * 缓存是否为空
   */
  fun isEmpty(): Boolean

  /**
   * 是否不为空
   */
  @JvmDefault
  fun isNotEmpty(): Boolean = !isEmpty()

  /**
   * dump cached entities
   */
  fun dump(): String

  /**
   * 缓存入库
   */
  fun flush(): Future<Unit>
}
