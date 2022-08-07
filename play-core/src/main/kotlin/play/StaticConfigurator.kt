package play

import play.util.reflect.Reflect

/**
 *
 * @author LiangZengle
 */
object StaticConfigurator {
  @JvmStatic
  fun <T> set(superType: Class<T>, implType: Class<out T>) {
    SystemProps.set(superType.name, implType.name)
  }

  @JvmStatic
  fun <T : Any> get(superType: Class<T>): T {
    return Reflect.getKotlinObjectOrNewInstance(SystemProps.getOrThrow(superType.name))
  }

  @JvmStatic
  fun <T : Any> getOrDefault(superType: Class<T>, defaultImpl: () -> T): T {
    return SystemProps.getOrNull(superType.name)?.let { Reflect.getKotlinObjectOrNewInstance(it) } ?: defaultImpl()
  }
}
