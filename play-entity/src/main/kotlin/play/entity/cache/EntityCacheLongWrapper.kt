package play.entity.cache

import play.entity.LongIdEntity
import play.util.collection.LongIterable

/**
 *
 *
 * @author LiangZengle
 */
internal class EntityCacheLongWrapper<E : LongIdEntity>(private val underlying: EntityCache<Long, E>) :
  EntityCacheLong<E>, EntityCache<Long, E> by underlying {

  override fun getAll(ids: LongIterable): List<E> {
    return getAll(ids.toJava())
  }
}
