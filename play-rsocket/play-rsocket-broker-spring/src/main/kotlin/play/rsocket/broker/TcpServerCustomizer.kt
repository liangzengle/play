package play.rsocket.broker

import reactor.netty.tcp.TcpServer

/**
 *
 *
 * @author LiangZengle
 */
fun interface TcpServerCustomizer {
  fun customize(server: TcpServer)
}
