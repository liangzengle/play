package play.rsocket.broker.rsocket

import io.rsocket.Payload
import io.rsocket.RSocket
import play.rsocket.broker.routing.RSocketLocator
import play.rsocket.metadata.RoutingMetadata

/**
 *
 * @author LiangZengle
 */
class RoutingRSocketFactory(
  private val rSocketLocator: RSocketLocator,
  private val routingMetaDataExtractor: (Payload) -> RoutingMetadata
) : RSocketFactory {

  override fun create(): RSocket {
    return RoutingRSocket(rSocketLocator, routingMetaDataExtractor)
  }
}
