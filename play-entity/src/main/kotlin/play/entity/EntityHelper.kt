package play.entity

import play.util.reflect.Reflect
import java.lang.reflect.Type

/**
 *
 * @author LiangZengle
 */
object EntityHelper {

  @JvmStatic
  fun getIdType(entityClass: Class<out Entity<*>>): Type {
    return if (LongIdEntity::class.java.isAssignableFrom(entityClass)) Long::class.java
    else if (IntIdEntity::class.java.isAssignableFrom(entityClass)) Int::class.java
    else Reflect.getTypeArg(entityClass, Entity::class.java, 0)
  }
}
