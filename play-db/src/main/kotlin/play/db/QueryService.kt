package play.db

import play.entity.Entity
import play.entity.cache.EntityCacheLoader
import play.util.concurrent.Future
import java.util.*
import javax.annotation.CheckReturnValue

interface QueryService : EntityCacheLoader {

  override fun <ID, E : Entity<ID>> loadById(id: ID, entityClass: Class<E>): Future<Optional<E>> {
    return findById(entityClass, id)
  }

  override fun <ID, E : Entity<ID>, C, C1 : C> loadAll(
    entityClass: Class<E>,
    initial: C1,
    f: (C1, E) -> C1
  ): Future<C> {
    return fold(entityClass, initial, f)
  }

  fun <ID, E : Entity<ID>> findById(entityClass: Class<E>, id: ID): Future<Optional<E>>

  fun <ID, E : Entity<ID>> listAll(entityClass: Class<E>): Future<List<E>>

  fun <ID, E : Entity<ID>> listIds(entityClass: Class<E>): Future<List<ID>>

  fun <ID, E : Entity<ID>, C, C1 : C> collectId(entityClass: Class<E>, c: C1, accumulator: (C1, ID) -> C1): Future<C>

  @CheckReturnValue
  fun <ID, E : Entity<ID>> foreach(entityClass: Class<E>, f: (E) -> Unit): Future<Unit> {
    return fold(entityClass, Unit) { _, e ->
      f(e)
    }
  }

  @CheckReturnValue
  fun <ID, E : Entity<ID>> foreach(entityClass: Class<E>, fields: List<String>, f: (ResultMap) -> Unit): Future<Unit> {
    return fold(entityClass, fields, Unit) { _, r ->
      f(r)
    }
  }

  @CheckReturnValue
  fun <ID, E : Entity<ID>, R, R1 : R> fold(
    entityClass: Class<E>,
    initial: R1,
    f: (R1, E) -> R1
  ): Future<R>

  @CheckReturnValue
  fun <ID, E : Entity<ID>, R, R1 : R> fold(
    entityClass: Class<E>,
    fields: List<String>,
    initial: R1,
    folder: (R1, ResultMap) -> R1
  ): Future<R>

}
