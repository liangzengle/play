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
  fun <T> loadClass(qualifiedName: String): Class<out T> {
    return loadClass(qualifiedName, Thread.currentThread().contextClassLoader)
  }

  @JvmStatic
  fun <T> loadClass(qualifiedName: String, classLoader: ClassLoader): Class<out T> {
    return loadClass<T>(qualifiedName, true, classLoader).unsafeCast()
  }

  @JvmStatic
  fun <T> loadClass(qualifiedName: String, initialize: Boolean): Class<out T> {
    return loadClass<T>(qualifiedName, initialize, Thread.currentThread().contextClassLoader).unsafeCast()
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
  fun isInstantiatable(clazz: Class<*>): Boolean {
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

inline fun <reified T> classOf(): Class<T> = T::class.java

inline fun <reified T> isAssignableFrom(clazz: Class<*>): Boolean = T::class.java.isAssignableFrom(clazz)

inline fun <reified T> Class<*>.isSubclassOf() = T::class.java.isAssignableFrom(this)
