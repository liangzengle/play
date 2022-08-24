package play.entity.cache

import play.entity.LongIdEntity
import play.util.collection.LongIterable
import java.util.*

/**
 *
 *
 * @author LiangZengle
 */
interface EntityCacheLong<E : LongIdEntity> : EntityCache<Long, E> {

  override fun get(id: Long): Optional<E>

  override fun getOrNull(id: Long): E?

  override fun getOrThrow(id: Long): E

  override fun getOrCreate(id: Long, creation: (Long) -> E): E

  override fun getCached(id: Long): Optional<E>

  fun getAll(ids: LongIterable): List<E>

  override fun delete(id: Long)

  override fun isCached(id: Long): Boolean
}
