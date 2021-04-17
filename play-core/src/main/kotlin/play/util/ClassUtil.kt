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
  fun <T> loadClass(fqcn: String): Class<out T> {
    return Class.forName(fqcn, true, null).unsafeCast()
  }

  fun <T> loadClass(fqcn: String, classLoader: ClassLoader): Class<out T> {
    return Class.forName(fqcn, true, classLoader).unsafeCast()
  }

  fun <T> loadClass(fqcn: String, initialize: Boolean): Class<out T> {
    return Class.forName(fqcn, initialize, null).unsafeCast()
  }

  fun <T> loadClass(fqcn: String, initialize: Boolean, classLoader: ClassLoader): Class<out T> {
    return Class.forName(fqcn, initialize, classLoader).unsafeCast()
  }

  fun isTopLevelConcreteClass(clazz: Class<*>): Boolean {
    return !clazz.isMemberClass &&
      !clazz.isLocalClass &&
      !clazz.isAnonymousClass &&
      clazz.declaringClass === null &&
      !clazz.isInterface && !clazz.isAbstract()
  }

  fun <T> newInstance(fqcn: String): T{
    return loadClass<T>(fqcn).createInstance()
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
