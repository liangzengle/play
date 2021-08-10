package play.res

import play.util.isAssignableFrom

internal object ResourceHelper {

  fun isSingletonResource(clazz: Class<*>): Boolean {
    return clazz.isAnnotationPresent(SingletonResource::class.java) || isConfig(clazz)
  }

  fun isConfig(clazz: Class<*>): Boolean {
    return isAssignableFrom<AbstractConfig>(clazz)
  }
}
