package play.res.reader

import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigParseOptions
import com.typesafe.config.ConfigRenderOptions
import com.typesafe.config.ConfigSyntax
import play.Log
import play.res.ResourceNotFoundException
import play.res.ResourcePath
import java.io.File
import java.net.URL
import java.util.*

class ConfigReader : Reader {

  override fun getURL(clazz: Class<*>): Result<URL> {
    return runCatching {
      val path = clazz.getAnnotation(ResourcePath::class.java)
        ?: throw IllegalArgumentException("[${clazz.name}]缺少@${ResourcePath::class.java.simpleName}")
      javaClass.classLoader.getResource(path.value)
        ?: throw ResourceNotFoundException("找不到[${clazz.name}]对应的配置文件[${path.value}]")
    }
  }

  override fun <T> read(clazz: Class<T>): List<T> {
    return try {
      val url = getURL(clazz).getOrThrow()
      val file = File(url.toURI())
      val fileExtension = file.extension
      val configSyntax = guessSyntax(fileExtension)
        ?: throw UnsupportedOperationException("Unsupported config file syntax: $fileExtension")
      val config =
        ConfigFactory.parseURL(url, ConfigParseOptions.defaults().setSyntax(configSyntax))
          .withFallback(ConfigFactory.parseString("{ id = 1 }"))
      val bean =
        Reader.readObject(config.root().render(ConfigRenderOptions.concise()), clazz)
      Collections.singletonList(bean)
    } catch (e: ResourceNotFoundException) {
      Log.error(e) { e.message }
      Collections.emptyList()
    } catch (e: Exception) {
      Log.error(e) { e.message }
      throw e
    }
  }

  private fun guessSyntax(fileExtension: String): ConfigSyntax? {
    return when (fileExtension) {
      "properties" -> ConfigSyntax.PROPERTIES
      "json" -> ConfigSyntax.JSON
      "conf" -> ConfigSyntax.CONF
      else -> null
    }
  }
}
