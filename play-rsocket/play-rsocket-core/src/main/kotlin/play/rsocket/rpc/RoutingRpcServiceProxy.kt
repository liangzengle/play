package play.rsocket.rpc

import play.rsocket.metadata.RoutingMetadata


/**
 *
 * @author LiangZengle
 */
abstract class RoutingRpcServiceProxy(@JvmField val routingMetadata: RoutingMetadata)
