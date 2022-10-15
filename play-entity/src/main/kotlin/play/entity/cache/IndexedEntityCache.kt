package play.entity.cache

import play.entity.Entity

interface IndexedEntityCache<IDX, ID, E : Entity<ID>> : EntityCache<ID, E> {
  fun getByIndex(index: IDX): MutableList<E>

  fun getIndexSize(index: IDX): Int
}
