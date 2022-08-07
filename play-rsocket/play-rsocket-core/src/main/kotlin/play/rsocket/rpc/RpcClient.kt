package play.rsocket.rpc

import play.rsocket.metadata.RoutingMetadata

/**
 *
 * @author LiangZengle
 */
interface RpcClient {

  fun <T : Any> getRpcService(serviceInterface: Class<T>, routing: RoutingMetadata): T

  fun <T : Any> getRpcService(serviceInterface: Class<T>, nodeId: Int): T {
    return getRpcService(serviceInterface, RoutingMetadata.oneNode(nodeId))
  }
}
