package play.db

import io.vavr.concurrent.Future

interface PersistService {
  fun insert(entity: Entity<*>): Future<out Any>

  fun update(entity: Entity<*>): Future<out Any>

  fun insertOrUpdate(entity: Entity<*>): Future<out Any>

  fun delete(entity: Entity<*>): Future<out Any>

  fun <ID, E : Entity<ID>> deleteById(id: ID, entityClass: Class<E>): Future<out Any>

  fun batchInsertOrUpdate(entities: Collection<Entity<*>>): Future<out Any>
}
