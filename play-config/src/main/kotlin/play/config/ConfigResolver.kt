package play.config

import java.io.File
import java.net.URI
import java.net.URL

open class ConfigResolver(val rootPath: URI) {
  companion object {
    const val ClassPathPrefix = "classpath:"

    @JvmStatic
    fun forPath(rootPath: String): ConfigResolver {
      val path = if (rootPath.last() == '\\' || rootPath.last() == '/') rootPath else "$rootPath/"
      val uri = if (path.startsWith(ClassPathPrefix)) {
        Thread.currentThread().contextClassLoader.getResource(path.substring(ClassPathPrefix.length))!!.toURI()
      } else {
        kotlin.runCatching { URI.create(path) }.getOrElse { File(path).toURI() }
      }
      return ConfigResolver(uri)
    }
  }

  open fun resolve(relativePath: String): Result<URL> {
    return runCatching { rootPath.resolve(relativePath).toURL() }
  }

  override fun toString(): String {
    return "ConfigResolver($rootPath)"
  }
}
