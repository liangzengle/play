package play

import com.typesafe.config.Config
import com.typesafe.config.ConfigRenderOptions
import play.util.json.Json
import play.util.primitive.ceilToInt

/**
 * Created by LiangZengle on 2020/2/15.
 */
class Configuration(private val config: Config) : Config by config {

  companion object {
    @JvmStatic
    fun from(config: Config): Configuration = Configuration(config)
  }

  operator fun plus(fallback: Configuration): Configuration {
    return Configuration(this.config.withFallback(fallback.config))
  }

  operator fun plus(fallback: Config): Configuration {
    return Configuration(this.config.withFallback(fallback))
  }

  fun toJson(): String = config.root().render(ConfigRenderOptions.concise())

  fun getConfiguration(path: String): Configuration = Configuration(config.getConfig(path))

  inline fun <reified T> to(): T = Json.to(toJson())

  inline fun <reified T> extract(path: String): T = Json.to(getConfiguration(path).toJson())

  @Suppress("UNCHECKED_CAST")
  fun <T> getClass(path: String): Class<T> = Class.forName(getString(path)) as Class<T>

  fun getThreadNum(path: String): Int {
    val value = getString(path)
    return when (value[0]) {
      'x', 'X' -> (sys.availableProcessors() * value.substring(1, value.length).toDouble()).ceilToInt()
      else -> value.toInt()
    }
  }
}

fun Config.toConfiguration() = Configuration(this)

operator fun Config.plus(other: Config): Config = withFallback(other)

fun Config.toJson() = root().render(ConfigRenderOptions.concise())
