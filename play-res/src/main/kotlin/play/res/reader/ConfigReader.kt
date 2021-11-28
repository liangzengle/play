package play.res.reader

import com.typesafe.config.ConfigFactory
import play.Log
import play.res.ResourcePath
import play.res.SourceNotFoundException
import play.util.TSConfig
import play.util.TSConfig.toJson
import java.net.URL
import java.util.*

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
        ?: throw SourceNotFoundException("找不到[${clazz.name}]对应的配置文件[${path.value}]")
    }
  }

  override fun <T> read(clazz: Class<T>): List<T> {
    return try {
      val url = getURL(clazz).getOrThrow()
      val config = ConfigFactory.parseURL(url).withFallback(ID).resolve()
      val bean = Reader.readObject(config.toJson(), clazz)
      listOf(bean)
    } catch (e: SourceNotFoundException) {
      Log.error(e) { e.message }
      Collections.emptyList()
    } catch (e: Exception) {
      Log.error(e) { e.message }
      throw e
    }
  }

  fun getAllURLs(clazz: Class<*>): Set<URL> {
    val url = getURL(clazz).getOrThrow()
    return TSConfig.getIncludedUrls(url)
  }
}
