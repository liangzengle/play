package play.net.netty.http

import play.net.http.HttpParameters
import java.util.*

class NettyHttpParameters internal constructor(private val parameters: Map<String, List<String>>) :
  HttpParameters() {

  override
  fun getMulti(name: String): List<String> {
    return parameters[name] ?: emptyList()
  }

  override fun getStringOptional(name: String): Optional<String> {
    return Optional.ofNullable(parameters[name]?.getOrNull(0))
  }

  override fun toString(): String {
    val b = StringBuilder(128)
    b.append("parameter: ").append('\n')
    parameters.forEach {
      b.append('\t').append(it.key).append('=')
      if (it.value.size > 1) {
        b.append(it.value)
      } else if (it.value.size == 1) {
        b.append(it.value[0])
      }
      b.append('\t')
    }
    return b.toString()
  }
}
