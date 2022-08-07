package play.rsocket.client

import io.rsocket.RSocket
import io.rsocket.SocketAcceptor
import io.rsocket.core.RSocketClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.ApplicationListener
import org.springframework.context.SmartLifecycle
import play.rsocket.client.event.RSocketBrokerAddressUpdateEvent
import play.rsocket.client.event.RSocketClientInitializedEvent
import play.rsocket.transport.SmartTransportFactory
import reactor.util.retry.Retry
import java.time.Duration
import java.util.concurrent.TimeoutException

/**
 *
 *
 * @author LiangZengle
 */
class BrokerRSocketManagerImpl(
  private val initialBrokerUris: Set<String>,
  private val socketAcceptor: SocketAcceptor,
  private val clientCustomizers: ObjectProvider<RSocketClientCustomizer>,
  private val transportFactory: SmartTransportFactory,
  private val eventPublisher: ApplicationEventPublisher
) : BrokerRSocketManager, SmartLifecycle, ApplicationListener<RSocketBrokerAddressUpdateEvent> {

  companion object {
    @JvmStatic
    private val logger = LoggerFactory.getLogger(BrokerRSocketManagerImpl::class.java)
  }

  private val rsocket = LoadbalancedBrokerRSocket(::newClient)

  override fun getRSocket(): RSocket {
    return rsocket
  }

  private fun newClient(uri: String): RSocketClient {
    val builder = RSocketClientBuilder()
      .acceptor(socketAcceptor)
      .transport(transportFactory.buildClient(uri))
      .connectRetry(Retry.backoff(10, Duration.ofSeconds(5)))
    clientCustomizers.forEach { it.customize(builder) }
    return builder.build()
  }

  override fun onApplicationEvent(event: RSocketBrokerAddressUpdateEvent) {
    rsocket.update(event.source.toSet())
  }

  override fun start() {
    rsocket.init(initialBrokerUris)
    rsocket.onConnected().subscribe {
      eventPublisher.publishEvent(RSocketClientInitializedEvent)
    }
    if (initialBrokerUris.isNotEmpty()) {
      rsocket.onConnected().timeout(Duration.ofSeconds(60))
        .doOnError(TimeoutException::class.java) {
          logger.error("Unable to connect broker in 60s")
        }
        .block()
    }
  }

  override fun stop() {
    rsocket.dispose()
  }

  override fun isRunning(): Boolean {
    return !rsocket.isDisposed
  }
}
