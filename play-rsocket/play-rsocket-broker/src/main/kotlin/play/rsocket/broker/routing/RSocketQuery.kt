package play.rsocket.broker.routing

import io.rsocket.RSocket
import play.rsocket.metadata.RoutingMetadata

/**
 *
 * @author LiangZengle
 */
interface RSocketQuery {
  fun query(routing: RoutingMetadata): List<RSocket>
}
