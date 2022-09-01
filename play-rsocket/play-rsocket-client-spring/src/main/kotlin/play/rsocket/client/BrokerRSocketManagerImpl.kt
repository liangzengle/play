package play.rsocket.client

import io.rsocket.RSocket
import io.rsocket.SocketAcceptor
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.ApplicationListener
import org.springframework.context.SmartLifecycle
import play.rsocket.client.event.RSocketBrokerAddressUpdateEvent
import play.rsocket.client.event.RSocketClientInitializedEvent
import play.rsocket.transport.SmartTransportFactory
import java.time.Duration
import java.util.concurrent.TimeoutException

/**
 *
 *
 * @author LiangZengle
 */
class BrokerRSocketManagerImpl(
  private val initialBrokerUris: Set<String>,
  private val connectTimeout: Duration?,
  socketAcceptor: SocketAcceptor,
  clientCustomizers: ObjectProvider<RSocketClientCustomizer>,
  transportFactory: SmartTransportFactory,
  private val eventPublisher: ApplicationEventPublisher
) : BrokerRSocketManager, SmartLifecycle, ApplicationListener<RSocketBrokerAddressUpdateEvent> {

  companion object {
    @JvmStatic
    private val logger = LoggerFactory.getLogger(BrokerRSocketManagerImpl::class.java)
  }

  private val rsocket = LoadbalancedBrokerRSocket(socketAcceptor, transportFactory, clientCustomizers)

  override fun getRSocket(): RSocket {
    return rsocket
  }

  override fun onApplicationEvent(event: RSocketBrokerAddressUpdateEvent) {
    rsocket.update(event.source.toSet())
  }

  override fun start() {
    rsocket.init(initialBrokerUris)
    rsocket.onConnected().subscribe {
      eventPublisher.publishEvent(RSocketClientInitializedEvent(it))
    }
    if (initialBrokerUris.isNotEmpty() && connectTimeout != null) {
      rsocket.onConnected().timeout(connectTimeout)
        .doOnError(TimeoutException::class.java) {
          logger.error("Unable to connect broker in {}s", connectTimeout.toSeconds())
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
