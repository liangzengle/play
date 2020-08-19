package play.db

import io.vavr.concurrent.Future
import io.vavr.control.Option
import javax.annotation.CheckReturnValue

interface QueryService {

  fun <ID, E : Entity<ID>> findById(id: ID, entityClass: Class<E>): Future<Option<E>>

  fun <ID, E : Entity<ID>> listAll(entityClass: Class<E>): Future<List<E>>

  @CheckReturnValue
  fun <ID, E : Entity<ID>> foreach(entityClass: Class<E>, f: (E) -> Unit): Future<Unit> {
    return fold(entityClass, Unit) { _, e ->
      f(e)
    }
  }

  @CheckReturnValue
  fun <ID, E : Entity<ID>, R> fold(entityClass: Class<E>, initial: R, f: (R, E) -> R): Future<R>

  fun <ID, E : Entity<ID>> listIds(entityClass: Class<E>): Future<List<ID>>

  fun <ID, E : Entity<ID>> query(
    entityClass: Class<E>,
    where: Option<String> = Option.none(),
    order: Option<String> = Option.none(),
    limit: Option<Int> = Option.none()
  ): Future<List<E>>

  fun <ID, E : Entity<ID>> query(
    entityClass: Class<E>,
    fields: List<String>,
    where: Option<String> = Option.none(),
    order: Option<String> = Option.none(),
    limit: Option<Int> = Option.none()
  ): Future<List<ResultMap>>

  @CheckReturnValue
  fun <ID, E : Entity<ID>> foreach(
    entityClass: Class<E>,
    fields: List<String>,
    where: Option<String> = Option.none(),
    order: Option<String> = Option.none(),
    limit: Option<Int> = Option.none(),
    f: (ResultMap) -> Unit
  ): Future<Unit> {
    return fold(entityClass, fields, where, order, limit, Unit) { _, r ->
      f(r)
      Unit
    }
  }

  @CheckReturnValue
  fun <ID, E : Entity<ID>, R> fold(
    entityClass: Class<E>,
    fields: List<String>,
    where: Option<String> = Option.none(),
    order: Option<String> = Option.none(),
    limit: Option<Int> = Option.none(),
    initial: R,
    folder: (R, ResultMap) -> R
  ): Future<R>
}
