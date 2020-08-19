package play.util.reflect

import com.google.common.reflect.TypeToken
import play.util.collection.EmptyObjectArray
import play.util.unsafeCast
import java.lang.reflect.*

/**
 * Created by LiangZengle on 2020/2/16.
 */
@Suppress("UNCHECKED_CAST")
object Reflect {

  fun <T> getClass(fqcn: String): Class<out T> {
    val clazz = Class.forName(fqcn)
    return clazz as Class<out T>
  }

  fun <T> getClass(fqcn: String, classLoader: ClassLoader): Class<out T> {
    val clazz = Class.forName(fqcn, false, classLoader)
    return clazz as Class<out T>
  }

  @Suppress("UnstableApiUsage")
  fun <T> getTypeArg(type: Class<out T>, superType: Class<T>, index: Int): Type {
    return getTypeArgs(type, superType)[index]
  }

  fun <T> getTypeArgs(type: Class<out T>, superType: Class<T>): Array<Type> {
    if (type.superclass == superType) {
      return type.genericSuperclass.unsafeCast<ParameterizedType>().actualTypeArguments
    }
    val directlyImplementedInterface =
      type.genericInterfaces.find { it is ParameterizedType && it.rawType == superType }
    if (directlyImplementedInterface != null) {
      return directlyImplementedInterface.unsafeCast<ParameterizedType>().actualTypeArguments
    }
    val runtimeType = TypeToken.of(type).getSupertype(superType).type
    return runtimeType.unsafeCast<ParameterizedType>().actualTypeArguments
  }

  fun <T> getRawClass(type: Type): Class<T> {
    return when (type) {
      is ParameterizedType -> getRawClass(type.rawType)
      is Class<*> -> type as Class<T>
      is TypeVariable<*> -> getRawClass(type.bounds[0])
      is WildcardType -> getRawClass(type.upperBounds[0])
      else -> throw IllegalStateException("Unexpected type: ${type.javaClass.name}")
    }
  }

  fun <T> createInstance(clazz: Class<out T>): T = clazz.getDeclaredConstructor().newInstance(*EmptyObjectArray)

  inline fun <reified T> createInstance(fqcn: String): T {
    val clazz = Class.forName(fqcn)
    if (!T::class.java.isAssignableFrom(clazz)) {
      throw ClassCastException("${clazz.name} can't assigned to ${T::class.java.name}")
    }
    return createInstance(clazz as Class<out T>)
  }
}

fun <T> Class<T>.getTypeArg(superType: Class<in T>, index: Int): Type {
  return Reflect.getTypeArg(this, superType, index)
}

fun <T> Type.getRawClass(): Class<T> = Reflect.getRawClass(this)

fun <T> Class<T>.createInstance(): T = Reflect.createInstance(this)

fun Class<*>.isPublic() = Modifier.isPublic(modifiers)

fun Class<*>.isProtected() = Modifier.isProtected(modifiers)

fun Class<*>.isPrivate() = Modifier.isPrivate(modifiers)

fun Class<*>.isAbstract() = Modifier.isAbstract(modifiers)

fun Class<*>.isStatic() = Modifier.isStatic(modifiers)

fun Class<*>.isFinal() = Modifier.isFinal(modifiers)

fun Class<*>.isStrict() = Modifier.isStrict(modifiers)

inline fun <reified T> classOf(): Class<T> = T::class.java

inline fun <reified T> isAssignable(clazz: Class<*>): Boolean = T::class.java.isAssignableFrom(clazz)

@Suppress("UNCHECKED_CAST")
fun <T> Field.getUnchecked(target: Any?): T? {
  trySetAccessible()
  return this.get(target) as T?
}

@Suppress("UNCHECKED_CAST")
fun <T : Any> Method.invokeUnchecked(target: Any?, vararg parameters: Any?): T? {
  trySetAccessible()
  return this.invoke(target, parameters) as T?
}
