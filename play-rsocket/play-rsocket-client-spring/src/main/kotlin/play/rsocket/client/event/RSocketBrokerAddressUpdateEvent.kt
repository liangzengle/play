package play.rsocket.client.event

import org.springframework.context.ApplicationEvent

/**
 *
 *
 * @author LiangZengle
 */
class RSocketBrokerAddressUpdateEvent(brokerUirs: List<String>) : ApplicationEvent(brokerUirs) {
  @Suppress("UNCHECKED_CAST")
  override fun getSource(): List<String> {
    return source as List<String>
  }
}
