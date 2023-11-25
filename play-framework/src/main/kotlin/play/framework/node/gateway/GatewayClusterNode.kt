package play.framework.node.gateway

import play.net.netty.NettyServerBuilder

class GatewayClusterNode {

  fun start() {
    NettyServerBuilder().port(7001)
  }
}
