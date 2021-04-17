@file:JvmName("ConfigUtil")
package play.util

import com.typesafe.config.Config
import com.typesafe.config.ConfigRenderOptions
import play.util.json.Json

fun Config.renderToJson(): String = root().render(ConfigRenderOptions.concise())

inline fun <reified T> Config.extract(path: String): T = Json.to(getConfig(path).renderToJson())
