package play.res.reader

import com.fasterxml.jackson.databind.DeserializationFeature
import play.res.ResourcePath
import play.res.ResourceUrlResolver
import play.res.deser.ImmutableCollectionModule
import play.util.json.Json
import java.net.URL

class JsonResourceReader(override val resolver: ResourceUrlResolver) : ResourceReader() {

  override val format: String = "json"

  override fun <T> read(clazz: Class<T>): Result<List<T>> {
    fun fallback(clazz: Class<*>): URL? {
      if (clazz.isAnnotationPresent(ResourcePath::class.java)) return null
      return getURL(trimClassicPostfix(clazz.simpleName)).getOrNull()
    }
    return getURL(clazz)
      .recoverCatching { e -> fallback(clazz) ?: throw e }
      .mapCatching { url -> readList(url, clazz) }
  }

  companion object {
    @JvmStatic
    val objectMapper = Json.copyObjectMapper()
      .configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE, true)
      .registerModule(ImmutableCollectionModule())

    @JvmStatic
    fun <T> readList(src: URL, elemType: Class<T>): List<T> {
      val type = objectMapper.typeFactory.constructCollectionType(List::class.java, elemType)
      return objectMapper.readValue(src, type)
    }

    @JvmStatic
    fun <T> readObject(src: String, type: Class<T>): T {
      return objectMapper.readValue(src, type)
    }
  }
}
