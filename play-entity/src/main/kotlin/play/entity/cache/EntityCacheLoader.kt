package play.entity.cache

import java.util.*
import javax.annotation.CheckReturnValue
import play.entity.Entity
import play.util.concurrent.Future

interface EntityCacheLoader {

  @CheckReturnValue
  fun <ID, E : Entity<ID>> loadById(id: ID, entityClass: Class<E>): Future<Optional<E>>

  @CheckReturnValue
  fun <ID, E : Entity<ID>, C, C1 : C> loadAll(entityClass: Class<E>, initial: C1, f: (C1, E) -> C1): Future<C>
}
