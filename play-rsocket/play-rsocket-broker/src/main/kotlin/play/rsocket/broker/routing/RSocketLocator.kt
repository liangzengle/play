package play.rsocket.broker.routing

import io.rsocket.RSocket
import play.rsocket.metadata.RoutingMetadata
import play.rsocket.metadata.RoutingType

/**
 *
 * @author LiangZengle
 */
interface RSocketLocator {

  fun supports(routingType: RoutingType): Boolean

  fun locate(routing: RoutingMetadata): RSocket
}
