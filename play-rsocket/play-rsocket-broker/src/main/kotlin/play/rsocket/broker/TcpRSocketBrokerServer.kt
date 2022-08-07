package play.rsocket.broker

import io.rsocket.transport.netty.server.CloseableChannel
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono
import java.net.InetSocketAddress
import java.time.Duration

/**
 *
 *
 * @author LiangZengle
 */
class TcpRSocketBrokerServer(private val channelMono: Mono<CloseableChannel>, private val bindTimeout: Duration) :
  RSocketBrokerServer {

  constructor(channelMono: Mono<CloseableChannel>) : this(channelMono, Duration.ofSeconds(5))

  companion object {
    private val logger = LoggerFactory.getLogger(TcpRSocketBrokerServer::class.java)
  }

  private var chanel: CloseableChannel? = null

  override fun start() {
    chanel = channelMono.block(bindTimeout)
    logger.info("TcpRSocketBrokerServer started, listening on {}", chanel?.address())
  }

  override fun stop() {
    chanel?.dispose()
    chanel = null
  }

  override fun address(): InetSocketAddress? {
    return chanel?.address()
  }
}
