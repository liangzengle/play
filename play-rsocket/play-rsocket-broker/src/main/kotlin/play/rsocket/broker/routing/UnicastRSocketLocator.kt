package play.rsocket.broker.routing

import io.rsocket.RSocket
import io.rsocket.loadbalance.LoadbalanceStrategy
import play.rsocket.broker.rsocket.UnknownRSocket
import play.rsocket.metadata.RoutingMetadata
import play.rsocket.metadata.RoutingType

/**
 *
 * @author LiangZengle
 */
class UnicastRSocketLocator(
  private val rSocketQuery: RSocketQuery,
  private val loadbalanceStrategy: LoadbalanceStrategy
) : RSocketLocator {

  override fun supports(routingType: RoutingType): Boolean {
    return routingType.isUnicast()
  }

  override fun locate(routing: RoutingMetadata): RSocket {
    val rSockets = rSocketQuery.query(routing)
    val size = rSockets.size
    if (size == 1) {
      return rSockets[0]
    }
    if (size > 1) {
      return loadbalanceStrategy.select(rSockets)
    }
    return UnknownRSocket(routing)
  }
}
