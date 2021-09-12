package play.res.reader

import com.fasterxml.jackson.databind.DeserializationFeature
import play.res.deser.ImmutableCollectionModule
import play.util.json.Json
import java.net.URL

interface Reader {

  fun getURL(clazz: Class<*>): Result<URL>

  fun <T> read(clazz: Class<T>): List<T>

  companion object {
    @JvmStatic
    val objectMapper = Json.mapper.copy()
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

