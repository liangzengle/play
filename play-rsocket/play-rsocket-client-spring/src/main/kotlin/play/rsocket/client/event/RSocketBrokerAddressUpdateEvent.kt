package play.rsocket.client.event

import org.springframework.context.ApplicationEvent

/**
 *
 *
 * @author LiangZengle
 */
class RSocketBrokerAddressUpdateEvent(brokerUris: List<String>) : ApplicationEvent(brokerUris) {
  @Suppress("UNCHECKED_CAST")
  override fun getSource(): List<String> {
    return source as List<String>
  }
}
