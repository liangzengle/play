package play.res

import java.io.File
import java.net.URI
import java.net.URL
import java.nio.file.InvalidPathException

open class ResourceUrlResolver(val rootPath: URI) {
  companion object {
    const val ClassPathPrefix = "classpath:"

    @JvmStatic
    fun forPath(rootPath: String): ResourceUrlResolver {
      val path = if (rootPath.last() == '\\' || rootPath.last() == '/') rootPath else "$rootPath/"
      val uri = if (path.startsWith(ClassPathPrefix)) {
        Thread.currentThread().contextClassLoader.getResource(path.substring(ClassPathPrefix.length))?.toURI()
          ?: throw InvalidPathException(rootPath, "Resource Not Found")
      } else {
        kotlin.runCatching { URI.create(path) }.getOrElse { File(path).toURI() }
      }
      return ResourceUrlResolver(uri)
    }
  }

  open fun resolve(relativePath: String): Result<URL> {
    return runCatching { rootPath.resolve(relativePath).toURL() }
  }

  override fun toString(): String {
    return "ResourceUrlResolver($rootPath)"
  }
}
