package play.rsocket.rpc

import play.rsocket.metadata.RoutingMetadata

/**
 *
 * @author LiangZengle
 */
abstract class RpcServiceStub {

  protected lateinit var routingMetadata: RoutingMetadata

  abstract fun serviceInterface(): Class<*>

  fun routingMetadata(routingMetadata: RoutingMetadata) {
    this.routingMetadata = routingMetadata
  }
}
