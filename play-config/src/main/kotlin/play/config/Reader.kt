package play.config

import com.fasterxml.jackson.core.JsonProcessingException
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigParseOptions
import com.typesafe.config.ConfigSyntax
import io.vavr.control.Try
import io.vavr.kotlin.`try`
import play.Log
import play.config.deser.ImmutableCollectionModule
import play.toJson
import play.util.json.Json
import java.io.File
import java.io.IOException
import java.net.URL
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

interface Reader {

  fun getURL(clazz: Class<*>): Try<URL>

  fun <T> read(clazz: Class<T>): List<T>
}

@Singleton
class ResourceReader : Reader {

  override fun getURL(clazz: Class<*>): Try<URL> {
    return `try` {
      val resource = clazz.getAnnotation(Resource::class.java)
        ?: throw IllegalArgumentException("[${clazz.name}]缺少@${Resource::class.java.simpleName}")
      javaClass.classLoader.getResource("/${resource.value}")
        ?: throw ResourceNotFoundException("找不到[${clazz.name}]对应的配置文件[${resource.value}]")
    }
  }

  override fun <T> read(clazz: Class<T>): List<T> {
    return try {
      val url = getURL(clazz).get()
      val file = File(url.toURI())
      val fileExtension = file.extension
      val configSyntax = guessSyntax(fileExtension)
        ?: throw UnsupportedOperationException("Unsupported config file syntax: $fileExtension")
      val config =
        ConfigFactory.parseURL(url, ConfigParseOptions.defaults().setSyntax(configSyntax))
          .withFallback(ConfigFactory.parseString("{ id = 1 }"))
      val bean = JsonConfigReader.jsonToObject(config.toJson(), clazz)
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

interface ConfigReader : Reader {
  val format: String

  val resolver: ConfigResolver

  override fun getURL(clazz: Class<*>): Try<URL> {
    val name = clazz.getAnnotation(ConfigPath::class.java)?.value ?: clazz.simpleName
    return resolver.resolve("$name.$format")
  }
}

@Singleton
class JsonConfigReader @Inject constructor(
  override val resolver: ConfigResolver
) : ConfigReader {

  override val format: String = "json"

  override fun <T> read(clazz: Class<T>): List<T> {
    return try {
      val url = getURL(clazz).get()
      jsonToList(url, clazz)
    } catch (e: JsonProcessingException) {
      throw e
    } catch (e: IOException) {
      Log.warn { "配置读取失败[${clazz.simpleName}]: ${e.javaClass.simpleName}(${e.message})" }
      emptyList()
    }
  }

  companion object {
    val objectMapper = Json.mapper.copy().registerModule(ImmutableCollectionModule())

    fun <T> jsonToList(src: URL, elemType: Class<T>): List<T> {
      val type = objectMapper.typeFactory.constructCollectionType(List::class.java, elemType)
      return objectMapper.readValue(src, type)
    }

    fun <T> jsonToObject(src: String, type: Class<T>): T {
      return objectMapper.readValue(src, type)
    }
  }
}
