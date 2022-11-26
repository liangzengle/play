package play.rsocket.client

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.NestedConfigurationProperty
import java.time.Duration

/**
 *
 *
 * @author LiangZengle
 */
@ConfigurationProperties(prefix = "rsocket.client")
class RSocketClientProperties(
  val brokers: List<String>,
  val connectTimeout: Duration?,
  @field: NestedConfigurationProperty val auth: Auth
) {

  class Auth {
    var type = ""
    var token = ""
  }
}
