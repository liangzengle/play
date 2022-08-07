package play.rsocket.metadata

/**
 *
 *
 * @author LiangZengle
 */
enum class RoutingType(@JvmField val id: Byte, @JvmField val broadcastType: BroadcastType) {
  UnicastToNode(1, BroadcastType.UNICAST),
  UnicastToRole(2, BroadcastType.UNICAST),
  MulticastToNodes(3, BroadcastType.MULTICAST),
  MulticastToRole(4, BroadcastType.MULTICAST),
  MulticastToRoles(5, BroadcastType.MULTICAST);

  fun isUnicast(): Boolean {
    return broadcastType == BroadcastType.UNICAST
  }

  fun isMulticast(): Boolean {
    return broadcastType == BroadcastType.MULTICAST
  }
}
