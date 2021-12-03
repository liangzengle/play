package play.entity.cache

import play.entity.Entity
import play.entity.EntityHelper
import play.entity.ObjId
import play.util.reflect.Reflect

/**
 *
 * @author LiangZengle
 */
abstract class MultiCacheExpireEvaluator<E : Entity<out ObjId>, K> {

  init {
    val entityClass = Reflect.getRawClassOfTypeArg<MultiCacheExpireEvaluator<*, *>, E>(
      javaClass,
      MultiCacheExpireEvaluator::class.java,
      0
    )
    val idType = EntityHelper.getIdType(entityClass)
    if (EntityCacheHelper.hasMultiKey(Reflect.getRawClass(idType))) {
      throw IllegalArgumentException()
    }
  }

  abstract fun canExpire(key: K): Boolean
}

object DefaultMultiCacheExpireEvaluator: MultiCacheExpireEvaluator<Entity<out ObjId>, Any>() {
  override fun canExpire(key: Any): Boolean {
    return true
  }
}
