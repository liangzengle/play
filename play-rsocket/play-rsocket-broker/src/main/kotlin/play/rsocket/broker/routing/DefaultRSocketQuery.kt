package play.rsocket.broker.routing

import io.rsocket.RSocket
import play.rsocket.metadata.RoutingMetadata
import play.rsocket.metadata.RoutingType

/**
 *
 *
 * @author LiangZengle
 */
class DefaultRSocketQuery(private val routingTable: RoutingTable) : RSocketQuery {
  override fun query(routing: RoutingMetadata): List<RSocket> {
    return when (routing.routingType) {
      RoutingType.UnicastToNode -> {
        val rSocket = routingTable.get(routing.nodeId)
        if (rSocket == null) emptyList() else listOf(rSocket)
      }

      RoutingType.UnicastToRole -> {
        routingTable.query(routing.role).toList()
      }

      RoutingType.MulticastToNodes -> {
        val rsockets = ArrayList<RSocket>(routing.nodeIds.size)
        for (nodeId in routing.nodeIds) {
          val rsocket = routingTable.get(nodeId)
          if (rsocket != null) {
            rsockets.add(rsocket)
          }
        }
        rsockets
      }

      RoutingType.MulticastToRole -> {
        routingTable.query(routing.role).toList()
      }

      RoutingType.MulticastToRoles -> {
        var result: Sequence<RSocket> = emptySequence()
        for (role in routing.roles) {
          result += routingTable.query(routing.role)
        }
        return result.toList()
      }
    }
  }
}
