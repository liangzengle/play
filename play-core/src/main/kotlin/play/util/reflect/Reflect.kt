package play.util.reflect

import com.google.common.collect.Iterables
import com.google.common.reflect.TypeToken
import play.util.EmptyClassArray
import play.util.EmptyObjectArray
import play.util.unsafeCast
import java.lang.reflect.*

/**
 * Created by LiangZengle on 2020/2/16.
 */
@Suppress("UNCHECKED_CAST")
object Reflect {

  val stackWalker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)

  @JvmStatic
  fun <T> getTypeArg(type: Class<out T>, superType: Class<T>, index: Int): Type {
    return getTypeArgs(type, superType)[index]
  }

  @JvmStatic
  @Suppress("UnstableApiUsage")
  fun <T> getTypeArgs(type: Class<out T>, superType: Class<T>): Array<Type> {
    if (type.superclass == superType) {
      return type.genericSuperclass.unsafeCast<ParameterizedType>().actualTypeArguments
    }
    if (superType.isInterface) {
      val directlyImplementedInterface =
        type.genericInterfaces.find { it is ParameterizedType && it.rawType == superType }
      if (directlyImplementedInterface != null) {
        return directlyImplementedInterface.unsafeCast<ParameterizedType>().actualTypeArguments
      }
    }
    val runtimeType = TypeToken.of(type).getSupertype(superType).type
    return runtimeType.unsafeCast<ParameterizedType>().actualTypeArguments
  }

  @JvmStatic
  tailrec fun <T> getRawClass(type: Type): Class<T> {
    return when (type) {
      is ParameterizedType -> getRawClass(type.rawType)
      is Class<*> -> type as Class<T>
      is TypeVariable<*> -> getRawClass(type.bounds[0])
      is WildcardType -> getRawClass(type.upperBounds[0])
      else -> throw IllegalStateException("Unexpected type: ${type.javaClass.name}")
    }
  }

  @JvmStatic
  fun <T, R> getRawClassOfTypeArg(type: Class<out T>, superType: Class<T>, index: Int): Class<R> {
    return getRawClass(getTypeArg(type, superType, index))
  }

  @JvmStatic
  fun <T> checkTypeArgEquals(type: Class<out T>, superType: Class<T>, index: Int, expectedType: Class<*>) {
    val actualType = getRawClassOfTypeArg<T, Any>(type, superType, index)
    check(actualType == expectedType) { "Unexpected type arg: $actualType" }
  }

  @JvmStatic
  fun <T : Any> getKotlinObjectOrNull(clazz: Class<T>): T? {
    return clazz.kotlin.objectInstance
  }

  @JvmStatic
  fun <T : Any> getKotlinObjectOrThrow(clazz: Class<T>): T {
    return clazz.kotlin.objectInstance ?: throw IllegalStateException("$clazz is not declared as `object`")
  }

  @JvmStatic
  fun <T> createInstance(clazz: Class<out T>): T = clazz.getDeclaredConstructor().newInstance(*EmptyObjectArray)

  @JvmStatic
  fun <T> createInstanceWithArgs(clazz: Class<out T>, vararg args: Any?): T {
    outer@
    for (ctor in clazz.declaredConstructors) {
      if (ctor.parameterCount != args.size) {
        continue
      }
      val parameterTypes = ctor.parameterTypes
      for (i in parameterTypes.indices) {
        val expectedType = getRawClass<Any>(parameterTypes[i])
        val argType = args[i]?.javaClass
        if (argType != null && !expectedType.isAssignableFrom(argType)) {
          break@outer
        }
      }
      return ctor.newInstance(*args).unsafeCast()
    }
    throw NoSuchMethodException()
  }

  @JvmStatic
  fun <T> createInstance(requiredType: Class<T>, fqcn: String): T {
    return createInstanceWithArgs(requiredType, fqcn, *EmptyObjectArray)
  }

  @JvmStatic
  fun <T> createInstanceWithArgs(requiredType: Class<T>, fqcn: String, vararg args: Any?): T {
    val clazz = Class.forName(fqcn)
    if (!requiredType.isAssignableFrom(clazz)) {
      throw ClassCastException("${clazz.name} can't assigned to ${requiredType.name}")
    }
    return createInstanceWithArgs(clazz as Class<out T>, *args)
  }

  inline fun <reified T> createInstance(fqcn: String): T {
    return createInstance(T::class.java, fqcn)
  }

  inline fun <reified T> createInstance(fqcn: String, vararg args: Any?): T {
    return createInstanceWithArgs(T::class.java, fqcn, *args)
  }

  @JvmStatic
  fun getCallerClass(): Class<*> {
    return stackWalker.walk { it.map { f -> f.declaringClass }.skip(2).findFirst().orElseThrow() }
  }

  @JvmStatic
  fun <T : Any> getFieldValue(field: Field, target: Any?): T? {
    field.trySetAccessible()
    return field.get(target) as T?
  }

  @JvmStatic
  fun <T : Any> invokeMethod(method: Method, target: Any?, vararg parameters: Any?): T? {
    method.trySetAccessible()
    return method.invoke(target, parameters) as T?
  }

  @JvmStatic
  fun <T : Any> invokeStaticMethodNoArgs(clazz: Class<*>, methodName: String): T? {
    return invokeMethodNoArgs(null, clazz, methodName)
  }

  @JvmStatic
  fun <T : Any> invokeMethodNoArgs(target: Any, methodName: String): T? {
    return invokeMethodNoArgs(target, target.javaClass, methodName)
  }

  @JvmStatic
  private fun <T : Any> invokeMethodNoArgs(target: Any?, targetClass: Class<*>, methodName: String): T? {
    var method: Method? = null
    var ex: NoSuchElementException? = null
    var superType = targetClass
    while (superType != Any::class.java) {
      try {
        method = superType.getDeclaredMethod(methodName, *EmptyClassArray)
      } catch (e: NoSuchElementException) {
        ex = e
        superType = superType.superclass
      }
    }
    if (method == null) {
      throw ex!!
    }
    method.trySetAccessible()
    return method.invoke(target, *EmptyObjectArray) as T?
  }

  @JvmStatic
  fun getAllFields(clazz: Class<*>): Iterable<Field> {
    var iterable = clazz.declaredFields.asIterable()
    var superType = clazz.superclass
    while (superType != Any::class.java) {
      iterable = Iterables.concat(superType.declaredFields.asIterable(), iterable)
      superType = superType.superclass
    }
    return iterable
  }

  @JvmStatic
  fun getAllMethods(clazz: Class<*>): Iterable<Method> {
    var iterable = clazz.declaredMethods.asIterable()
    var superType = clazz.superclass
    while (superType != Any::class.java) {
      iterable = Iterables.concat(superType.declaredMethods.asIterable(), iterable)
      superType = superType.superclass
    }
    return iterable
  }
}
