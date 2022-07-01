package play.entity.cache

import play.entity.Entity
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface EntityCacheLoader {

  fun <ID, E : Entity<ID>> loadById(entityClass: Class<E>, id: ID): Mono<E>

  fun <ID, E : Entity<ID>> loadAll(entityClass: Class<E>, ids: Iterable<ID>): Flux<E>

  fun <ID, E : Entity<ID>> loadAll(entityClass: Class<E>): Flux<E>

  fun <IDX, ID : Any, E : Entity<ID>> loadIdsByIndex(
    entityClass: Class<E>,
    indexName: String,
    indexValue: IDX
  ): Flux<ID>
}
