package play.config

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationFeature
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigParseOptions
import com.typesafe.config.ConfigRenderOptions
import com.typesafe.config.ConfigSyntax
import play.Log
import play.config.deser.ImmutableCollectionModule
import play.util.json.Json
import java.io.File
import java.io.IOException
import java.net.URL
import java.util.*
import javax.inject.Inject

interface Reader {

  fun getURL(clazz: Class<*>): Result<URL>

  fun <T> read(clazz: Class<T>): List<T>
}

class ResourceReader : Reader {

  override fun getURL(clazz: Class<*>): Result<URL> {
    return runCatching {
      val resource = clazz.getAnnotation(Resource::class.java)
        ?: throw IllegalArgumentException("[${clazz.name}]缺少@${Resource::class.java.simpleName}")
      javaClass.classLoader.getResource("/${resource.value}")
        ?: throw ResourceNotFoundException("找不到[${clazz.name}]对应的配置文件[${resource.value}]")
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
      val bean = JsonConfigReader.jsonToObject(config.root().render(ConfigRenderOptions.concise()), clazz)
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

abstract class ConfigReader : Reader {
  abstract val format: String

  abstract val resolver: ConfigResolver

  override fun getURL(clazz: Class<*>): Result<URL> {
    val name = clazz.getAnnotation(ConfigPath::class.java)?.value ?: clazz.simpleName
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

class JsonConfigReader @Inject constructor(
  override val resolver: ConfigResolver
) : ConfigReader() {

  override val format: String = "json"

  override fun <T> read(clazz: Class<T>): List<T> {
    return try {
      val originalResult = getURL(clazz)
      var result = originalResult
      if (originalResult.isFailure && !clazz.isAnnotationPresent(ConfigPath::class.java)) {
        result = getURL(trimClassicPostfix(clazz.simpleName))
      }
      if (result.isFailure) {
        originalResult.getOrThrow()
      }
      val url = result.getOrThrow()
      jsonToList(url, clazz)
    } catch (e: JsonProcessingException) {
      throw e
    } catch (e: IOException) {
      Log.warn { "配置读取失败[${clazz.simpleName}]: ${e.javaClass.simpleName}(${e.message})" }
      emptyList()
    }
  }

  companion object {
    @JvmStatic
    val objectMapper = Json.mapper.copy()
      .configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE, true)
      .registerModule(ImmutableCollectionModule())

    @JvmStatic
    fun <T> jsonToList(src: URL, elemType: Class<T>): List<T> {
      val type = objectMapper.typeFactory.constructCollectionType(List::class.java, elemType)
      return objectMapper.readValue(src, type)
    }

    @JvmStatic
    fun <T> jsonToObject(src: String, type: Class<T>): T {
      return objectMapper.readValue(src, type)
    }
  }
}
