package play.db

import play.entity.Entity
import play.entity.cache.EntityCacheLoader
import play.util.concurrent.Future
import java.util.*
import javax.annotation.CheckReturnValue

interface QueryService : EntityCacheLoader {

  fun <ID, E : Entity<ID>> listAll(entityClass: Class<E>): Future<List<E>>

  fun <ID, E : Entity<ID>> listIds(entityClass: Class<E>): Future<List<ID>>

  fun <ID, E : Entity<ID>> query(
    entityClass: Class<E>,
    where: Optional<String> = Optional.empty(),
    order: Optional<String> = Optional.empty(),
    limit: Optional<Int> = Optional.empty()
  ): Future<List<E>> {
    return fold(entityClass, where, order, limit, LinkedList()) { list, e -> list.apply { add(e) } }
  }

  fun <ID, E : Entity<ID>> query(
    entityClass: Class<E>,
    fields: List<String>,
    where: Optional<String> = Optional.empty(),
    order: Optional<String> = Optional.empty(),
    limit: Optional<Int> = Optional.empty()
  ): Future<List<ResultMap>> {
    return fold(
      entityClass,
      fields,
      where,
      order,
      limit,
      LinkedList<ResultMap>(),
    ) { list: LinkedList<ResultMap>, resultMap ->
      list.apply {
        add(resultMap)
      }
    }
  }

  @CheckReturnValue
  override fun <ID, E : Entity<ID>> foreach(entityClass: Class<E>, f: (E) -> Unit): Future<Unit> {
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
    }
  }

  @CheckReturnValue
  fun <ID, E : Entity<ID>, R, R1 : R> fold(
    entityClass: Class<E>,
    initial: R1,
    f: (R1, E) -> R1
  ): Future<R> {
    return fold(entityClass, Optional.empty(), Optional.empty(), Optional.empty(), initial, f)
  }

  @CheckReturnValue
  fun <ID, E : Entity<ID>, R, R1 : R> fold(
    entityClass: Class<E>,
    where: Optional<String>,
    order: Optional<String>,
    limit: Optional<Int>,
    initial: R1,
    folder: (R1, E) -> R1
  ): Future<R>

  @CheckReturnValue
  fun <ID, E : Entity<ID>, R, R1 : R> fold(
    entityClass: Class<E>,
    fields: List<String>,
    initial: R1,
    folder: (R1, ResultMap) -> R1
  ): Future<R> {
    return fold(entityClass, fields, Optional.empty(), Optional.empty(), Optional.empty(), initial, folder)
  }

  @CheckReturnValue
  fun <ID, E : Entity<ID>, R, R1 : R> fold(
    entityClass: Class<E>,
    fields: List<String>,
    where: Optional<String>,
    order: Optional<String>,
    limit: Optional<Int>,
    initial: R1,
    folder: (R1, ResultMap) -> R1
  ): Future<R>
}
