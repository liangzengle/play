package play.entity.cache

import play.entity.Entity

/**
 *
 * @author LiangZengle
 */
abstract class EntityInitializer<E : Entity<*>> {

  internal fun initialize(entity: E) {
    beforeInitialize(entity)
    entity.initialize()
    afterInitialize(entity)
  }

  open fun beforeInitialize(entity: E) {
  }

  open fun afterInitialize(entity: E) {
  }
}

internal object DefaultEntityInitializer : EntityInitializer<Entity<Any>>()
