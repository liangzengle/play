package play.util.logging

import kotlin.reflect.KClass

interface PlayLoggerFactory {

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
