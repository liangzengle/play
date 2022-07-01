package play.db

import play.entity.Entity
import play.entity.cache.EntityCacheLoader
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface QueryService : EntityCacheLoader {
  override fun <ID, E : Entity<ID>> loadById(entityClass: Class<E>, id: ID): Mono<E> {
    return findById(entityClass, id)
  }

  override fun <ID, E : Entity<ID>> loadAll(entityClass: Class<E>, ids: Iterable<ID>): Flux<E> {
    return Flux.fromIterable(ids).flatMap { loadById(entityClass, it) }
  }

  override fun <ID, E : Entity<ID>> loadAll(entityClass: Class<E>): Flux<E> {
    return queryAll(entityClass)
  }

  fun <ID, E : Entity<ID>> findById(entityClass: Class<E>, id: ID): Mono<E>

  fun <ID, E : Entity<ID>> queryAll(entityClass: Class<E>): Flux<E>

  fun <ID, E : Entity<ID>> queryIds(entityClass: Class<E>): Flux<ID>

  fun <ID, E : Entity<ID>> query(entityClass: Class<E>, fields: List<String>): Flux<ResultMap>
}
