package play.entity

import play.util.LambdaClassValue
import play.util.reflect.Reflect
import java.lang.reflect.Type

/**
 *
 * @author LiangZengle
 */
object EntityHelper {

  private val idTypeCache = LambdaClassValue { type ->
    @Suppress("UNCHECKED_CAST")
    if (LongIdEntity::class.java.isAssignableFrom(type)) Long::class.java
    else if (IntIdEntity::class.java.isAssignableFrom(type)) Int::class.java
    else if (StringIdEntity::class.java.isAssignableFrom(type)) String::class.java
    else Reflect.getTypeArg(type as Class<out Entity<*>>, Entity::class.java, 0)
  }

  @JvmStatic
  fun getIdType(entityClass: Class<out Entity<*>>): Type {
    return idTypeCache.get(entityClass)
  }

  @JvmStatic
  fun getIdClass(entityClass: Class<out Entity<*>>): Class<*> {
    return Reflect.getRawClass<Any>(getIdType(entityClass))
  }
}
