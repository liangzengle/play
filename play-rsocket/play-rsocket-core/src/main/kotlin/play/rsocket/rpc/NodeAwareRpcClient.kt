package play.rsocket.rpc

import play.rsocket.metadata.RoutingMetadata

/**
 *
 * @author LiangZengle
 */
abstract class NodeAwareRpcClient : RpcClient {
  var nodeId = 0
  var localServiceProvider: ((Class<*>) -> Any?)? = null

  @Suppress("UNCHECKED_CAST")
  final override fun <T : Any> getRpcService(serviceInterface: Class<T>, routing: RoutingMetadata): T {
    if (routing.nodeId > 0 && routing.nodeId == nodeId) {
      val impl = localServiceProvider?.invoke(serviceInterface)
      if (impl != null) {
        return impl as T
      }
    }
    return getRpcService0(serviceInterface, routing)
  }

  protected abstract fun <T : Any> getRpcService0(serviceInterface: Class<T>, routing: RoutingMetadata): T
}
