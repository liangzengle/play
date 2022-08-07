package play.rsocket.broker

import java.net.InetSocketAddress

/**
 *
 *
 * @author LiangZengle
 */
interface RSocketBrokerServer {

  fun start()

  fun stop()

  fun address(): InetSocketAddress?
}
