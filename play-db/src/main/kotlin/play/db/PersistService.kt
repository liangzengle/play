package play.db

import play.entity.Entity
import play.entity.cache.EntityCacheWriter
import play.util.concurrent.Future

interface PersistService : EntityCacheWriter {
  fun <ID, E : Entity<ID>> deleteIfDeleted(id: ID, entityClass: Class<E>): Future<out Any>
}
