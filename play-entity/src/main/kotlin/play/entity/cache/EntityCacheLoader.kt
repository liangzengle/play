package play.entity.cache

import java.util.*
import javax.annotation.CheckReturnValue
import play.entity.Entity
import play.util.concurrent.Future

interface EntityCacheLoader {

  fun <ID, E : Entity<ID>> findById(id: ID, entityClass: Class<E>): Future<Optional<E>>

  @CheckReturnValue
  fun <ID, E : Entity<ID>> foreach(entityClass: Class<E>, f: (E) -> Unit): Future<Unit>
}
