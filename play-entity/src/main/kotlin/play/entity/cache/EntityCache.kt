package play.entity.cache

import play.entity.Entity
import play.util.concurrent.Future
import java.util.*

interface EntityCache<ID, E : Entity<ID>> {

  operator fun invoke(id: ID): E = getOrThrow(id)

  /**
   * 对应实体类
   */
  val entityClass: Class<E>

  /**
   * 从缓存中获取实体对象，如果不存在则从数据库中加载
   */
  fun get(id: ID): Optional<E>

  /**
   * 从缓存中获取实体对象，如果不存在则从数据库中加载
   */
  fun getOrNull(id: ID): E?

  /**
   * 从缓存中获取实体对象，如果不存在则从数据库中加载，如果数据中不存在则抛出异常: [NoSuchElementException]
   */
  @Throws(NoSuchElementException::class)
  fun getOrThrow(id: ID): E

  /**
   * 从缓存中获取实体对象，如果不存在则从数据库中加载，如果数据库中不存在则创建
   */
  fun getOrCreate(id: ID, creation: (ID) -> E): E

  /**
   * 从缓存中获取实体对象，不从数据库中加载
   */
  fun getCached(id: ID): Optional<E>

  /**
   * 从缓存中获取实体对，如果不存在则从数据库中加载
   */
  fun getAll(ids: Iterable<ID>): MutableList<E>

  /**
   * 创建实体，如果实体已经存在则抛异常: [EntityExistsException]
   */
  @Throws(EntityExistsException::class)
  fun create(e: E): E

  /**
   * 从缓存和数据库中移除实体
   */
  fun delete(e: E)

  /**
   * 从缓存和数据库中移除实体
   */
  fun delete(id: ID)

  /**
   * 缓存中的实体数量
   */
  fun size(): Int

  /**
   * 对应的实体是否在缓存中
   */
  fun isCached(id: ID): Boolean

  /**
   * 缓存是否为空
   */
  fun isEmpty(): Boolean

  /**
   * 是否不为空
   */
  fun isNotEmpty(): Boolean = !isEmpty()

  /**
   * dump cached entities
   */
  fun dump(): String

  /**
   * 缓存入库
   */
  fun persist(): Future<Unit>
}
