package play.db

import play.entity.Entity
import play.util.concurrent.Future
import play.util.empty
import play.util.emptyInt
import java.util.*
import javax.annotation.CheckReturnValue

interface ParameterizedQueryService<P> : QueryService {

  fun <ID, E : Entity<ID>> query(
    entityClass: Class<E>,
    where: Optional<P> = empty(),
    order: Optional<P> = empty(),
    limit: OptionalInt = emptyInt()
  ): Future<List<E>> {
    return fold(entityClass, where, order, limit, LinkedList()) { list, e -> list.apply { add(e) } }
  }

  fun <ID, E : Entity<ID>> query(
    entityClass: Class<E>,
    fields: List<String>,
    where: Optional<P> = empty(),
    order: Optional<P> = empty(),
    limit: OptionalInt = emptyInt()
  ): Future<List<ResultMap>> {
    return fold(
      entityClass,
      fields,
      where,
      order,
      limit,
      LinkedList<ResultMap>()
    ) { list, resultMap ->
      list.apply {
        add(resultMap)
      }
    }
  }

  @CheckReturnValue
  fun <ID, E : Entity<ID>> foreach(
    entityClass: Class<E>,
    fields: List<String>,
    where: Optional<P>,
    order: Optional<P> = empty(),
    limit: OptionalInt = emptyInt(),
    f: (ResultMap) -> Unit
  ): Future<Unit> {
    return fold(entityClass, fields, where, order, limit, Unit) { _, r ->
      f(r)
    }
  }

  @CheckReturnValue
  fun <ID, E : Entity<ID>, R, R1 : R> fold(
    entityClass: Class<E>,
    where: Optional<P>,
    order: Optional<P>,
    limit: OptionalInt,
    initial: R1,
    folder: (R1, E) -> R1
  ): Future<R>

  @CheckReturnValue
  fun <ID, E : Entity<ID>, R, R1 : R> fold(
    entityClass: Class<E>,
    fields: List<String>,
    where: Optional<P>,
    order: Optional<P>,
    limit: OptionalInt,
    initial: R1,
    folder: (R1, ResultMap) -> R1
  ): Future<R>
}
