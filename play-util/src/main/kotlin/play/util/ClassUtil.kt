package play.util

import play.util.reflect.Reflect
import java.lang.reflect.Type
import kotlin.reflect.jvm.internal.impl.builtins.jvm.JavaToKotlinClassMap
import kotlin.reflect.jvm.internal.impl.name.FqName

/**
 * @author LiangZengle
 */
object ClassUtil {
  @JvmStatic
  fun javaToKotlin(qualifiedName: String): String {
    return JavaToKotlinClassMap.INSTANCE.mapJavaToKotlin(FqName(qualifiedName))?.asSingleFqName()?.asString()
      ?: qualifiedName
  }

  @JvmStatic
  fun kotlinToJava(qualifiedName: String): String {
    return JavaToKotlinClassMap.INSTANCE.mapKotlinToJava(FqName(qualifiedName).toUnsafe())?.asSingleFqName()?.asString()
      ?: qualifiedName
  }

  @JvmStatic
  fun wrap(type: Class<*>): Class<*> {
    return if (type.isPrimitive) getPrimitiveWrapperType(type) else type
  }

  @JvmStatic
  fun unwrap(type: Class<*>): Class<*> {
    return getPrimitiveType(type) ?: type
  }

  @JvmStatic
  fun wrap(qualifiedName: String): String {
    return when (qualifiedName) {
      "int" -> "java.lang.Integer"
      "long" -> "java.lang.Long"
      "byte" -> "java.lang.Byte"
      "boolean" -> "java.lang.Boolean"
      "short" -> "java.lang.Short"
      "double" -> "java.lang.Double"
      "float" -> "java.lang.Float"
      "char" -> "java.lang.Character"
      "void" -> "java.lang.Void"
      else -> qualifiedName
    }
  }


  @JvmStatic
  fun unwrap(qualifiedName: String): String {
    return when (qualifiedName) {
      "java.lang.Integer" -> "int"
      "java.lang.Long" -> "long"
      "java.lang.Byte" -> "byte"
      "java.lang.Boolean" -> "boolean"
      "java.lang.Short" -> "short"
      "java.lang.Double" -> "double"
      "java.lang.Float" -> "float"
      "java.lang.Character" -> "char"
      "java.lang.Void" -> "void"
      else -> qualifiedName
    }
  }

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
    return if (type.isPrimitive) type.kotlin.javaObjectType else type
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

  @JvmStatic
  fun <T> load(qualifiedName: String): Class<T> {
    return Reflect.loadClass(qualifiedName)
  }

  @JvmStatic
  fun <T> load(qualifiedName: String, initialize: Boolean): Class<T> {
    return Reflect.loadClass(qualifiedName, initialize)
  }

  @JvmStatic
  fun <T> load(qualifiedName: String, initialize: Boolean, classLoader: ClassLoader): Class<T> {
    return Reflect.loadClass(qualifiedName, initialize, classLoader)
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
