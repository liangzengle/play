package play.rsocket.broker

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.boot.context.properties.NestedConfigurationProperty

/**
 *
 *
 * @author LiangZengle
 */
@ConfigurationProperties(prefix = "rsocket.broker")
@ConstructorBinding
class RSocketBrokerProperties(
  val id: Int,
  val host: String,
  val port: Int,
  @field:NestedConfigurationProperty val auth: Auth
) {

  class Auth {
    var type = "token"
    var token = ""
  }
}
