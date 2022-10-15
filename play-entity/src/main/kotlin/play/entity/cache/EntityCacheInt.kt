package play.entity.cache

import play.entity.IntIdEntity
import play.util.collection.IntIterable
import java.util.*

/**
 *
 *
 * @author LiangZengle
 */
interface EntityCacheInt<E : IntIdEntity> : EntityCache<Int, E> {

  override fun get(id: Int): Optional<E>

  override fun getOrNull(id: Int): E?

  override fun getOrThrow(id: Int): E

  override fun getOrCreate(id: Int, creation: (Int) -> E): E

  override fun getCached(id: Int): Optional<E>

  fun getAll(ids: IntIterable): MutableList<E>

  override fun delete(id: Int)

  override fun isCached(id: Int): Boolean
}
