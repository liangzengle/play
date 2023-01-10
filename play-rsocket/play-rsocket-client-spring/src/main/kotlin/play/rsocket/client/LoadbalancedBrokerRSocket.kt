package play.rsocket.client

import com.google.common.collect.ImmutableSet
import io.rsocket.Payload
import io.rsocket.RSocket
import io.rsocket.SocketAcceptor
import io.rsocket.exceptions.ConnectionErrorException
import io.rsocket.exceptions.InvalidException
import io.rsocket.transport.ClientTransport
import org.eclipse.collections.impl.list.mutable.FastList
import org.reactivestreams.Publisher
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectProvider
import play.rsocket.loadbalance.RandomLoadbalanceStrategy
import play.rsocket.transport.SmartTransportFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks
import java.net.ConnectException
import java.nio.channels.ClosedChannelException
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

/**
 *
 * @author LiangZengle
 */
class LoadbalancedBrokerRSocket(
  private val socketAcceptor: SocketAcceptor,
  private val transportFactory: SmartTransportFactory,
  private val clientCustomizers: ObjectProvider<RSocketClientCustomizer>
) : RSocket {

  companion object {
    @JvmStatic
    private val logger = LoggerFactory.getLogger(LoadbalancedBrokerRSocket::class.java)
  }

  private var brokerUris = emptySet<String>()
  private val activeRSocketMap = ConcurrentHashMap<String, RSocket>()
  private var activeRSocketList = emptyList<RSocket>()
  private val loadbalanceStrategy = RandomLoadbalanceStrategy()
  private val transportPool = ConcurrentHashMap<String, ClientTransport>()

  private val connected = Sinks.one<String>()

  @Volatile
  private var disposed = false

  fun init(brokerUris: Set<String>) {
    this.brokerUris = ImmutableSet.copyOf(brokerUris)
    brokerUris.forEach(::connect)
  }

  fun update(brokerUris: Set<String>) {
    this.brokerUris = ImmutableSet.copyOf(brokerUris)
    brokerUris.asSequence().filterNot { activeRSocketMap.containsKey(it) }.forEach(::connect)
  }

  fun onConnected(): Mono<String> {
    return connected.asMono()
  }

  override fun dispose() {
    disposed = true
    activeRSocketMap.values.forEach { it.dispose() }
  }

  override fun isDisposed(): Boolean {
    return activeRSocketList.all { it.isDisposed }
  }

  private fun connect0(uri: String): Mono<RSocket> {
    val transport = transportPool.computeIfAbsent(uri) { transportFactory.buildClient(it) }
    val builder = RSocketClientBuilder().acceptor(socketAcceptor).transport(uri, transport)
    clientCustomizers.forEach { it.customize(builder) }
    val client = builder.build()
    return client.source()
  }

  private fun connect(uri: String) {
    if (disposed) {
      return
    }
    val resolvingRSocket = connect0(uri)
    resolvingRSocket.subscribe(
      { onRSocketConnected(uri, it) },
      {
        logger.error("Can not establish connection with {}", uri, it)
        Mono.delay(Duration.ofSeconds(5)).subscribe { connect(uri) }
      }
    )
  }

  private fun reconnect(uri: String) {
    if (disposed) {
      return
    }
    if (!brokerUris.contains(uri)) {
      logger.debug("Give up reconnecting broker[{}], uri is invalid.", uri)
      return
    }
    logger.debug("Reconnecting broker: {}", uri)
    Mono.delay(Duration.ofSeconds(5))
      .flatMap { connect0(uri) }
      .subscribe(
        { onRSocketConnected(uri, it) },
        { reconnect(uri) }
      )
  }

  private fun onRSocketConnected(uri: String, socket: RSocket) {
    activeRSocketMap[uri] = socket
    if (disposed) {
      socket.dispose()
      return
    }
    activeRSocketList = FastList(activeRSocketMap.values)
    logger.info("Broker connected: {}", uri)
    connected.tryEmitValue(uri)

    socket.onClose().doFinally { onRSocketDisconnected(uri) }.subscribe()
  }

  private fun onRSocketDisconnected(uri: String) {
    if (activeRSocketMap.remove(uri) != null) {
      activeRSocketList = FastList(activeRSocketMap.values)
      logger.info("Broker disconnected: {}", uri)
      reconnect(uri)
    }
  }

  private fun onRSocketUnreachable(socket: RSocket) {
    val uri = activeRSocketMap.entries.find { it.value === socket }?.key ?: return
    if (activeRSocketMap.remove(uri, socket)) {
      activeRSocketList = FastList(activeRSocketMap.values)
      logger.info("Broker unreachable: {}", uri)
      reconnect(uri)
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
