package play.rsocket.client

import io.rsocket.Payload
import io.rsocket.RSocket
import io.rsocket.core.RSocketClient
import io.rsocket.exceptions.ConnectionErrorException
import io.rsocket.exceptions.InvalidException
import org.eclipse.collections.impl.list.mutable.FastList
import org.reactivestreams.Publisher
import org.slf4j.LoggerFactory
import play.rsocket.loadbalance.RandomLoadbalanceStrategy
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks
import java.net.ConnectException
import java.nio.channels.ClosedChannelException
import java.util.concurrent.ConcurrentHashMap

/**
 *
 *
 * @author LiangZengle
 */
class LoadbalancedBrokerRSocket(private val connector: (String) -> RSocketClient) : RSocket {

  companion object {
    @JvmStatic
    private val logger = LoggerFactory.getLogger(LoadbalancedBrokerRSocket::class.java)
  }

  private val activeRSocketMap = ConcurrentHashMap<String, RSocket>()
  private var activeRSocketList = emptyList<RSocket>()
  private val loadbalanceStrategy = RandomLoadbalanceStrategy()

  private val connected = Sinks.one<String>()

  fun init(brokerUris: Set<String>) {
    brokerUris.forEach(::connect)
  }

  fun update(brokerUirs: Set<String>) {
    brokerUirs.asSequence().filterNot { activeRSocketMap.containsKey(it) }.forEach(::connect)
  }

  fun onConnected(): Mono<String> {
    return connected.asMono()
  }

  override fun dispose() {
    activeRSocketList.forEach { it.dispose() }
  }

  override fun isDisposed(): Boolean {
    return activeRSocketList.all { it.isDisposed }
  }

  private fun connect(uri: String): Mono<RSocket> {
    val source = connector(uri).source()
    source.subscribe { socket ->
      onRSocketConnected(uri, socket)
    }
    return source
  }

  private fun onRSocketConnected(uri: String, socket: RSocket) {
    val prev = activeRSocketMap.putIfAbsent(uri, socket)
    if (prev == null) {
      activeRSocketList = FastList(activeRSocketMap.values)
    } else {
      socket.dispose()
    }
    logger.info("Connected to broker: {}", uri)
    connected.tryEmitValue(uri)
  }

  private fun onRSocketUnreachable(socket: RSocket) {
    val uri = activeRSocketMap.entries.find { it.value === socket }?.key ?: return
    if (activeRSocketMap.remove(uri, socket)) {
      activeRSocketList = FastList(activeRSocketMap.values)
      if (!socket.isDisposed) {
        socket.dispose()
      }
      logger.info("Disconnect broker: {}", uri)
    }
  }

  private fun select(): RSocket {
    val rsockets = activeRSocketList
    if (rsockets.isEmpty()) return InvalidRSocket
    return loadbalanceStrategy.select(rsockets)
  }

  private fun shouldResume(exception: Throwable): Boolean {
    return exception is ConnectionErrorException || exception is ConnectException || exception is ClosedChannelException
  }

  override fun fireAndForget(payload: Payload): Mono<Void> {
    val rSocket = select()
    return rSocket.fireAndForget(payload).onErrorResume(::shouldResume) {
      onRSocketUnreachable(rSocket)
      fireAndForget(payload)
    }
  }

  override fun requestResponse(payload: Payload): Mono<Payload> {
    val rSocket = select()
    return rSocket.requestResponse(payload).onErrorResume(::shouldResume) {
      onRSocketUnreachable(rSocket)
      requestResponse(payload)
    }
  }

  override fun requestStream(payload: Payload): Flux<Payload> {
    val rSocket = select()
    return rSocket.requestStream(payload).onErrorResume(::shouldResume) {
      onRSocketUnreachable(rSocket)
      requestStream(payload)
    }
  }

  override fun requestChannel(payloads: Publisher<Payload>): Flux<Payload> {
    val rSocket = select()
    return rSocket.requestChannel(payloads).onErrorResume(::shouldResume) {
      onRSocketUnreachable(rSocket)
      requestChannel(Flux.from(payloads))
    }
  }

  override fun metadataPush(payload: Payload): Mono<Void> {
    val rSocket = select()
    return rSocket.metadataPush(payload).onErrorResume(::shouldResume) {
      onRSocketUnreachable(rSocket)
      metadataPush(payload)
    }
  }

  private object InvalidRSocket : RSocket {
    private val EXCEPTION = InvalidException("No rsocket available")

    override fun fireAndForget(payload: Payload): Mono<Void> {
      return Mono.error(EXCEPTION)
    }

    override fun requestResponse(payload: Payload): Mono<Payload> {
      return Mono.error(EXCEPTION)
    }

    override fun requestStream(payload: Payload): Flux<Payload> {
      return Flux.error(EXCEPTION)
    }

    override fun requestChannel(payloads: Publisher<Payload>): Flux<Payload> {
      return Flux.error(EXCEPTION)
    }

    override fun metadataPush(payload: Payload): Mono<Void> {
      return Mono.error(EXCEPTION)
    }
  }
}
