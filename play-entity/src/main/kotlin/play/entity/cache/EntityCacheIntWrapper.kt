package play.entity.cache

import play.entity.IntIdEntity
import play.util.collection.IntIterable

/**
 *
 *
 * @author LiangZengle
 */
internal class EntityCacheIntWrapper<E : IntIdEntity>(private val underlying: EntityCache<Int, E>) : EntityCacheInt<E>,
  EntityCache<Int, E> by underlying {
  override fun getAll(ids: IntIterable): MutableList<E> {
    return getAll(ids.toJava())
  }
}
