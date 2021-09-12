package play.res.reader

import com.fasterxml.jackson.core.JsonProcessingException
import play.Log
import play.res.ResourcePath
import play.res.ResourceUrlResolver
import java.io.IOException

class JsonResourceReader(override val resolver: ResourceUrlResolver) : ResourceReader() {

  override val format: String = "json"

  override fun <T> read(clazz: Class<T>): List<T> {
    return try {
      val originalResult = getURL(clazz)
      var result = originalResult
      if (originalResult.isFailure && !clazz.isAnnotationPresent(ResourcePath::class.java)) {
        result = getURL(trimClassicPostfix(clazz.simpleName))
      }
      if (result.isFailure) {
        originalResult.getOrThrow()
      }
      val url = result.getOrThrow()
      Reader.readList(url, clazz)
    } catch (e: JsonProcessingException) {
      throw e
    } catch (e: IOException) {
      Log.warn { "配置读取失败[${clazz.simpleName}]: ${e.javaClass.simpleName}(${e.message})" }
      emptyList()
    }
  }
}
