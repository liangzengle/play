package play.util.logging

import kotlin.reflect.KClass

object PlayLoggerManager {

  private var defaultFactory: PlayLoggerFactory = SlfLoggerFactory

  @JvmStatic
  fun setFactory(factory: PlayLoggerFactory) {
    defaultFactory = factory
  }

  @JvmStatic
  fun getLogger(name: String): PlayLogger = defaultFactory.getLogger(name)

  @JvmStatic
  fun getLogger(clazz: Class<*>): PlayLogger = defaultFactory.getLogger(clazz)

  @JvmStatic
  fun getLogger(kclass: KClass<*>): PlayLogger = defaultFactory.getLogger(kclass)
}
