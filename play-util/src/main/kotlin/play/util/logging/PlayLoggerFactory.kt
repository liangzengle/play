package play.util.logging

import kotlin.reflect.KClass

interface PlayLoggerFactory {

  companion object Default {

    private var defaultFactory: PlayLoggerFactory = JdkLoggerFactory

    fun setFactory(factory: PlayLoggerFactory) {
      defaultFactory = factory
    }

    fun getLogger(name: String): PlayLogger = defaultFactory.getLogger(name)

    fun getLogger(clazz: Class<*>): PlayLogger = defaultFactory.getLogger(clazz)

    fun getLogger(kclass: KClass<*>): PlayLogger = defaultFactory.getLogger(kclass)
  }

  fun getLogger(name: String): PlayLogger

  fun getLogger(clazz: Class<*>): PlayLogger = getLogger(clazz.name)

  fun getLogger(kclass: KClass<*>): PlayLogger {
    return if (kclass.isCompanion) {
      getLogger(kclass.java.declaringClass)
    } else {
      getLogger(kclass.java)
    }
  }
}
