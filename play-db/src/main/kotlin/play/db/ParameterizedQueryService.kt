package play.db

import play.entity.Entity
import play.util.empty
import play.util.emptyInt
import reactor.core.publisher.Flux
import java.util.*

interface ParameterizedQueryService<P : Any> : QueryService {

  override fun <ID, E : Entity<ID>> query(entityClass: Class<E>, fields: List<String>): Flux<ResultMap> {
    return query(entityClass, fields, empty(), empty(), emptyInt())
  }

  fun <ID, E : Entity<ID>> query(
    entityClass: Class<E>,
    where: Optional<P>,
    order: Optional<P>,
    limit: OptionalInt,
  ): Flux<E>

  fun <ID, E : Entity<ID>> query(
    entityClass: Class<E>,
    fields: List<String>,
    where: Optional<P>,
    order: Optional<P>,
    limit: OptionalInt
  ): Flux<ResultMap>
}
