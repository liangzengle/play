package play.entity.cache

import play.entity.Entity
import play.entity.ObjId
import play.entity.ObjIdEntity
import play.util.concurrent.Future
import java.util.*
import javax.annotation.CheckReturnValue

interface EntityCacheLoader {

  @CheckReturnValue
  fun <ID, E : Entity<ID>> loadById(id: ID, entityClass: Class<E>): Future<Optional<E>>

  @CheckReturnValue
  fun <ID, E : Entity<ID>> loadAll(ids: Iterable<ID>, entityClass: Class<E>): Future<List<E>>

  @CheckReturnValue
  fun <ID, E : Entity<ID>, C, C1 : C> loadAll(entityClass: Class<E>, initial: C1, f: (C1, E) -> C1): Future<C>

  @CheckReturnValue
  fun <K, ID : ObjId, E : ObjIdEntity<ID>> listMultiIds(
    entityClass: Class<E>,
    keyName: String,
    keyValue: K
  ): Future<List<ID>>
}
