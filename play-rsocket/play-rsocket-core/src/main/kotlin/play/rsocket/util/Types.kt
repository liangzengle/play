package play.rsocket.util

import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.TypeVariable
import java.lang.reflect.WildcardType

/**
 *
 *
 * @author LiangZengle
 */
internal object Types {

  fun getRawClass(type: Type): Class<*> {
    return when (type) {
      is ParameterizedType -> getRawClass(type.rawType)
      is Class<*> -> type
      is TypeVariable<*> -> getRawClass(type.bounds[0])
      is WildcardType -> getRawClass(type.upperBounds[0])
      else -> throw IllegalStateException("Unexpected type: ${type.javaClass.name}")
    }
  }

  @JvmStatic
  fun isVoid(type: Type): Boolean = type == Void.TYPE || type == Void::class.java || type == Unit.javaClass
}
