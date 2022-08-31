package play.kryo.util

import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.TypeVariable
import java.lang.reflect.WildcardType

/**
 *
 *
 * @author LiangZengle
 */
object TypeUtil {

  fun getRawClass(type: Type): Class<*> {
    return when (type) {
      is ParameterizedType -> getRawClass(type.rawType)
      is Class<*> -> type
      is TypeVariable<*> -> getRawClass(type.bounds[0])
      is WildcardType -> getRawClass(type.upperBounds[0])
      else -> throw IllegalStateException("Unexpected type: ${type.javaClass.name}")
    }
  }
}
