package play.db

import play.util.concurrent.Future
import java.util.*
import javax.annotation.CheckReturnValue

interface QueryService {

  fun <ID, E : Entity<ID>> findById(id: ID, entityClass: Class<E>): Future<Optional<E>>

  fun <ID, E : Entity<ID>> listAll(entityClass: Class<E>): Future<List<E>>

  fun <ID, E : Entity<ID>> listIds(entityClass: Class<E>): Future<List<ID>>

  fun <ID, E : Entity<ID>> query(
    entityClass: Class<E>,
    where: Optional<String> = Optional.empty(),
    order: Optional<String> = Optional.empty(),
    limit: Optional<Int> = Optional.empty()
  ): Future<List<E>>

  fun <ID, E : Entity<ID>> query(
    entityClass: Class<E>,
    fields: List<String>,
    where: Optional<String> = Optional.empty(),
    order: Optional<String> = Optional.empty(),
    limit: Optional<Int> = Optional.empty()
  ): Future<List<ResultMap>>

  @CheckReturnValue
  fun <ID, E : Entity<ID>> foreach(entityClass: Class<E>, f: (E) -> Unit): Future<Unit> {
    return fold(entityClass, Unit) { _, e ->
      f(e)
    }
  }

  @CheckReturnValue
  fun <ID, E : Entity<ID>> foreach(
    entityClass: Class<E>,
    fields: List<String>,
    where: Optional<String> = Optional.empty(),
    order: Optional<String> = Optional.empty(),
    limit: Optional<Int> = Optional.empty(),
    f: (ResultMap) -> Unit
  ): Future<Unit> {
    return fold(entityClass, fields, where, order, limit, Unit) { _, r ->
      f(r)
      Unit
    }
  }

  @CheckReturnValue
  fun <ID, E : Entity<ID>, R> fold(entityClass: Class<E>, initial: R, f: (R, E) -> R): Future<R>

  @CheckReturnValue
  fun <ID, E : Entity<ID>, R> fold(
    entityClass: Class<E>,
    fields: List<String>,
    where: Optional<String> = Optional.empty(),
    order: Optional<String> = Optional.empty(),
    limit: Optional<Int> = Optional.empty(),
    initial: R,
    folder: (R, ResultMap) -> R
  ): Future<R>
}
