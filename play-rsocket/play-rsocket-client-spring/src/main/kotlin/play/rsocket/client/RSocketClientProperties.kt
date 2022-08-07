package play.rsocket.client

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.boot.context.properties.NestedConfigurationProperty

/**
 *
 *
 * @author LiangZengle
 */
@ConstructorBinding
@ConfigurationProperties(prefix = "rsocket.client")
class RSocketClientProperties(val brokers: List<String>, @field: NestedConfigurationProperty val auth: Auth) {

  class Auth {
    var type = ""
    var token = ""
  }
}
