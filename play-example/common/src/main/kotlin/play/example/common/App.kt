package play.example.common

import play.Log
import play.SystemProps
import play.util.concurrent.LoggingUncaughtExceptionHandler

/**
 *
 * @author LiangZengle
 */
abstract class App {
  init {
    loadSystemProperties()
    Thread.setDefaultUncaughtExceptionHandler(LoggingUncaughtExceptionHandler)
  }

  private fun loadSystemProperties() {
    for (resource in App::class.java.classLoader.getResources(".jvmopts")) {
      resource.openStream().bufferedReader().lines().map { it.trim() }.filter { it.startsWith("-D") }.forEach {
        val i = it.indexOf('=')
        val key = it.substring(2, i)
        val value = it.substring(i + 1)
        val prev = SystemProps.setIfAbsent(key.trim(), value.trim())
        if (prev == null) {
          Log.debug("set system property: $key=$value")
        }
      }
    }
  }
}
