package play.entity.cache

import play.entity.Entity

interface IndexedEntityCache<IDX, ID : Any, E : Entity<ID>> : EntityCache<ID, E> {
  fun getByIndex(index: IDX): List<E>

  fun getIndexSize(index: IDX): Int
}
