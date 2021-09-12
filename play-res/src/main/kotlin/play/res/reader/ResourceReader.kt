package play.res.reader

import play.res.ResourcePath
import play.res.ResourceUrlResolver
import java.net.URL

abstract class ResourceReader : Reader {
  abstract val format: String

  abstract val resolver: ResourceUrlResolver

  override fun getURL(clazz: Class<*>): Result<URL> {
    val name = clazz.getAnnotation(ResourcePath::class.java)?.value ?: clazz.simpleName
    return resolver.resolve("$name.$format")
  }

  fun getURL(fileNameNoExtension: String): Result<URL> {
    return resolver.resolve("$fileNameNoExtension.$format")
  }

  companion object {
    @JvmStatic
    fun trimClassicPostfix(simpleName: String): String {
      if (simpleName.endsWith("Config")) {
        return simpleName.substring(0, simpleName.length - 6)
      }
      if (simpleName.endsWith("Resource")) {
        return simpleName.substring(0, simpleName.length - 8)
      }
      return simpleName
    }
  }
}
