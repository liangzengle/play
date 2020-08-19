package play.config

import io.vavr.control.Try
import io.vavr.kotlin.Try
import java.io.File
import java.net.URI
import java.net.URL

class ConfigResolver(val rootPath: URI) {
  companion object {
    const val ClassPathPrefix = "classpath:"

    @JvmStatic
    fun forPath(rootPath: String): ConfigResolver {
      val path = if (rootPath.last() == '\\' || rootPath.last() == '/') rootPath else "$rootPath/"
      val uri = if (path.startsWith(ClassPathPrefix)) {
        Thread.currentThread().contextClassLoader.getResource(path.substring(ClassPathPrefix.length))!!.toURI()
      } else {
        Try { URI.create(path) }.getOrElse(File(path).toURI())
      }
      return ConfigResolver(uri)
    }
  }

  fun resolve(relativePath: String): Try<URL> {
    return Try { rootPath.resolve(relativePath).toURL() }
  }

  override fun toString(): String {
    return "ConfigResolver($rootPath)"
  }
}
