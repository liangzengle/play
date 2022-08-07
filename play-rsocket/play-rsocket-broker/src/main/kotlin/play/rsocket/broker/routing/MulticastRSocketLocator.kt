package play.rsocket.broker.routing

import io.rsocket.RSocket
import play.rsocket.broker.rsocket.MulticastRSocket
import play.rsocket.metadata.RoutingMetadata
import play.rsocket.metadata.RoutingType

/**
 *
 * @author LiangZengle
 */
class MulticastRSocketLocator(private val rSocketQuery: RSocketQuery) : RSocketLocator {

  override fun supports(routingType: RoutingType): Boolean {
    return routingType.isMulticast()
  }

  override fun locate(routing: RoutingMetadata): RSocket {
    val sockets = rSocketQuery.query(routing)
    return MulticastRSocket(sockets)
  }
}
