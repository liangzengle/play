package play.entity.cache

import play.entity.Entity
import reactor.core.publisher.Mono

interface EntityCacheWriter {
  fun insert(entity: Entity<*>): Mono<out Any>

  fun update(entity: Entity<*>): Mono<out Any>

  fun insertOrUpdate(entity: Entity<*>): Mono<out Any>

  fun delete(entity: Entity<*>): Mono<out Any>

  fun <ID, E : Entity<ID>> deleteById(id: ID, entityClass: Class<E>): Mono<out Any>

  fun batchInsertOrUpdate(entities: Collection<Entity<*>>): Mono<out Any>

  fun <ID, E : Entity<ID>> update(entityClass: Class<E>, id: ID, field: String, value: Any): Mono<out Any>
}
