package play.db.memory

import io.vavr.concurrent.Future
import io.vavr.control.Option
import play.db.Entity
import play.db.Repository
import play.db.ResultMap
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import javax.annotation.CheckReturnValue
import javax.inject.Singleton

@Suppress("UNCHECKED_CAST")
@Singleton
class MemoryRepository : Repository {
  private val caches: ConcurrentMap<Class<*>, ConcurrentMap<Any, Entity<*>>> = ConcurrentHashMap()

  private fun getMap(entityClass: Class<*>): ConcurrentMap<Any, Entity<*>> {
    val map = caches[entityClass]
    return map ?: caches.computeIfAbsent(entityClass) { ConcurrentHashMap() }
  }

  override fun insert(entity: Entity<*>): Future<out Any> {
    val prev = getMap(entity.javaClass).putIfAbsent(entity.id(), entity)
    if (prev != null) {
      throw IllegalStateException("${entity.javaClass.simpleName}(${entity.id()})已经存在")
    }
    return Future.successful(1)
  }

  override fun update(entity: Entity<*>): Future<out Any> {
    return Future.successful(Unit)
  }

  override fun insertOrUpdate(entity: Entity<*>): Future<out Any> {
    getMap(entity.javaClass).putIfAbsent(entity.id(), entity)
    return Future.successful(Unit)
  }

  override fun delete(entity: Entity<*>): Future<out Any> {
    getMap(entity.javaClass).remove(entity.id())
    return Future.successful(Unit)
  }

  override fun <ID, E : Entity<ID>> deleteById(id: ID, entityClass: Class<E>): Future<out Any> {
    getMap(entityClass).remove(id)
    return Future.successful(Unit)
  }

  override fun batchInsertOrUpdate(entities: Collection<Entity<*>>): Future<out Any> {
    return Future.successful(Unit)
  }

  override fun <ID, E : Entity<ID>> findById(id: ID, entityClass: Class<E>): Future<Option<E>> {
    val e = getMap(entityClass)[id]
    return Future.successful(Option.of(e) as Option<E>)
  }

  override fun <ID, E : Entity<ID>> listAll(entityClass: Class<E>): Future<List<E>> {
    return Future.successful(getMap(entityClass).values.toList() as List<E>)
  }

  override fun <ID, E : Entity<ID>> listIds(entityClass: Class<E>): Future<List<ID>> {
    return Future.successful(getMap(entityClass).keys.toList() as List<ID>)
  }

  override fun <ID, E : Entity<ID>> query(
    entityClass: Class<E>,
    where: Option<String>,
    order: Option<String>,
    limit: Option<Int>
  ): Future<List<E>> {
    return Future.successful(emptyList())
  }

  override fun <ID, E : Entity<ID>> query(
    entityClass: Class<E>,
    fields: List<String>,
    where: Option<String>,
    order: Option<String>,
    limit: Option<Int>
  ): Future<List<ResultMap>> {
    return Future.successful(emptyList())
  }

  @CheckReturnValue
  override fun <ID, E : Entity<ID>, R> fold(entityClass: Class<E>, initial: R, f: (R, E) -> R): Future<R> {
    val value = getMap(entityClass).values.fold(initial) { acc, entity -> f(acc, entity as E) }
    return Future.successful(value)
  }

  @CheckReturnValue
  override fun <ID, E : Entity<ID>, R> fold(
    entityClass: Class<E>,
    fields: List<String>,
    where: Option<String>,
    order: Option<String>,
    limit: Option<Int>,
    initial: R,
    folder: (R, ResultMap) -> R
  ): Future<R> {
    return Future.successful(initial)
  }

  override fun close() {
  }
}
