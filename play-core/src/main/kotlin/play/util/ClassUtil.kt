package play.util

import play.util.reflect.Reflect
import java.lang.reflect.Field
import java.lang.reflect.Member
import java.lang.reflect.Modifier
import java.lang.reflect.Type

/**
 * @author LiangZengle
 */
object ClassUtil {
  @JvmStatic
  fun isPrimitiveOrWrapper(type: Class<*>): Boolean {
    return getPrimitiveType(type) != null
  }

  @JvmStatic
  fun getPrimitiveType(type: Class<*>): Class<*>? {
    val primitiveType = type.kotlin.javaPrimitiveType
    return if (primitiveType == Void.TYPE) null else primitiveType
  }

  @JvmStatic
  fun getPrimitiveWrapperType(type: Class<*>): Class<*> {
    return type.kotlin.javaObjectType
  }

  @JvmStatic
  fun getPrimitiveDefaultValue(type: Class<*>): Any {
    val primitiveType = getPrimitiveType(type) ?: throw IllegalArgumentException("$type is not Primitive Type")
    return when (primitiveType.name) {
      "boolean" -> false
      "byte" -> 0.toByte()
      "short" -> 0.toShort()
      "int" -> 0
      "long" -> 0L
      "float" -> 0f
      "double" -> 0.0
      "char" -> 0.toChar()
      else -> throw IllegalStateException("should not happen")
    }
  }

  @JvmStatic
  fun <T> loadClass(qualifiedName: String): Class<out T> {
    return loadClass(qualifiedName, javaClass.classLoader)
  }

  @JvmStatic
  fun <T> loadClass(qualifiedName: String, classLoader: ClassLoader): Class<out T> {
    return loadClass<T>(qualifiedName, true, classLoader).unsafeCast()
  }

  @JvmStatic
  fun <T> loadClass(qualifiedName: String, initialize: Boolean): Class<out T> {
    return loadClass<T>(qualifiedName, initialize, javaClass.classLoader).unsafeCast()
  }

  @JvmStatic
  fun <T> loadClass(qualifiedName: String, initialize: Boolean, classLoader: ClassLoader): Class<out T> {
    return Class.forName(qualifiedName, initialize, classLoader).unsafeCast()
  }

  /**
   * 判断是否可创建实例
   *
   * @param clazz
   * @return
   */
  @JvmStatic
  fun isInstantiable(clazz: Class<*>): Boolean {
    return !clazz.isLocalClass && !clazz.isAnonymousClass && !clazz.isInterface && !clazz.isAbstract()
      && !clazz.isEnum && !(clazz.isMemberClass && !clazz.isStatic())
  }

  @JvmStatic
  fun <T> newInstance(fqcn: String): T {
    return loadClass<T>(fqcn).createInstance()
  }
}

fun <T> Class<T>.getTypeArg(superType: Class<in T>, index: Int): Type {
  return Reflect.getTypeArg(this, superType, index)
}

fun <T> Type.getRawClass(): Class<T> = Reflect.getRawClass(this)

fun <T> Class<T>.createInstance(): T = Reflect.newInstance(this)

fun Class<*>.isPublic() = Modifier.isPublic(modifiers)

fun Class<*>.isProtected() = Modifier.isProtected(modifiers)

fun Class<*>.isPrivate() = Modifier.isPrivate(modifiers)

fun Class<*>.isAbstract() = Modifier.isAbstract(modifiers)

fun Class<*>.isStatic() = Modifier.isStatic(modifiers)

fun Class<*>.isFinal() = Modifier.isFinal(modifiers)

fun Class<*>.isStrict() = Modifier.isStrict(modifiers)

inline fun <reified T> classOf(): Class<T> = T::class.java

inline fun <reified T> isAssignableFrom(clazz: Class<*>): Boolean = T::class.java.isAssignableFrom(clazz)

inline fun <reified T> Class<*>.isSubclassOf() = T::class.java.isAssignableFrom(this)

fun Member.isPublic() = Modifier.isPublic(modifiers)

fun Member.isProtected() = Modifier.isProtected(modifiers)

fun Member.isPrivate() = Modifier.isPrivate(modifiers)

fun Member.isAbstract() = Modifier.isAbstract(modifiers)

fun Member.isStatic() = Modifier.isStatic(modifiers)

fun Member.isFinal() = Modifier.isFinal(modifiers)

fun Member.isStrict() = Modifier.isStrict(modifiers)

fun Field.isTransient() = Modifier.isTransient(modifiers)
