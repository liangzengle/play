package play.res.reader

import com.typesafe.config.ConfigFactory
import play.res.ResourceNotFoundException
import play.res.ResourcePath
import play.util.TSConfigs
import play.util.TSConfigs.toJson
import java.net.URL

class ConfigReader : Reader {
  companion object {
    @JvmStatic
    private val ID = ConfigFactory.parseString("{ id = 1 }")
  }

  override fun getURL(clazz: Class<*>): Result<URL> {
    return runCatching {
      val path = clazz.getAnnotation(ResourcePath::class.java)
        ?: throw IllegalArgumentException("[${clazz.name}]缺少@${ResourcePath::class.java.simpleName}")
      javaClass.classLoader.getResource(path.value)
        ?: throw ResourceNotFoundException("找不到[${clazz.name}]对应的配置文件[${path.value}]")
    }
  }

  override fun <T> read(clazz: Class<T>): Result<List<T>> {
    return getURL(clazz).mapCatching { url ->
      val config = ConfigFactory.parseURL(url).withFallback(ID).resolve()
      val bean = JsonResourceReader.readObject(config.toJson(), clazz)
      listOf(bean)
    }
  }

  fun getAllURLs(clazz: Class<*>): Set<URL> {
    val url = getURL(clazz).getOrThrow()
    return TSConfigs.getIncludedUrls(url, true)
  }
}
