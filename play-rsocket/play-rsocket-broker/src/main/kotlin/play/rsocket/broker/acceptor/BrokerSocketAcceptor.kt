package play.rsocket.broker.acceptor

import io.rsocket.ConnectionSetupPayload
import io.rsocket.DuplexConnection
import io.rsocket.RSocket
import io.rsocket.SocketAcceptor
import io.rsocket.broker.rsocket.ErrorOnDisconnectRSocket
import io.rsocket.exceptions.InvalidSetupException
import org.slf4j.LoggerFactory
import play.rsocket.broker.routing.RoutingTable
import play.rsocket.broker.rsocket.RSocketFactory
import play.rsocket.metadata.RouteSetupMetadata
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.SignalType
import java.net.InetSocketAddress
import java.util.function.Consumer

/**
 *
 * @author LiangZengle
 */
class BrokerSocketAcceptor(
  val brokerId: Int,
  private val routingTable: RoutingTable,
  private val routeSetupMetadataExtractor: (ConnectionSetupPayload) -> RouteSetupMetadata?,
  private val receivingRSocketFactory: RSocketFactory
) : SocketAcceptor {
  companion object {
    private val logger = LoggerFactory.getLogger(BrokerSocketAcceptor::class.java)
  }

  override fun accept(setup: ConnectionSetupPayload, sendingSocket: RSocket): Mono<RSocket> {
    try {
      val remoteAddress = getRemoteAddress(sendingSocket)
      val routeSetup =
        routeSetupMetadataExtractor(setup) ?: return Mono.error(InvalidSetupException("Illegal setup frame"))
      val id = routeSetup.nodeId
      val role = routeSetup.role
      if (id <= 0 || role <= 0) {
        return Mono.error(InvalidSetupException("Illegal nodeId or role: nodeId=$id, role=$role"))
      }
      val wrappedSendingSocket = wrapSendingSocket(sendingSocket, id)
      val prev = routingTable.put(id, wrappedSendingSocket, role)
      if (prev != null && !prev.isDisposed) {
//        prev.dispose()
//        logger.info("Replace socket: {}@{}", id, remoteAddress)
      } else {
        logger.info("Accept socket: {}@{}", id, remoteAddress)
      }
      val cleanUpAction = Consumer<SignalType> {
        routingTable.remove(id, wrappedSendingSocket)
        logger.info("Remove socket from Routing Table: {}", id)
      }
      return finalize(sendingSocket, cleanUpAction)
    } catch (e: Exception) {
      return Mono.error(e)
    }
  }

  private fun wrapSendingSocket(sendingSocket: RSocket, id: Int): RSocket {
    val rSocket = ErrorOnDisconnectRSocket(sendingSocket)
    rSocket.onClose().doFinally { logger.info("Closing socket: {}", id) }
    return rSocket
  }

  private fun finalize(sendingSocket: RSocket, cleanUpAction: Consumer<SignalType>): Mono<RSocket> {
    val receivingSocket = receivingRSocketFactory.create()
    Flux.firstWithSignal(sendingSocket.onClose(), receivingSocket.onClose())
      .doFinally(cleanUpAction)
      .subscribe()
    return Mono.just(receivingSocket)
  }

  private fun getRemoteAddress(socket: RSocket): String? {
    try {
      val connectionField = socket.javaClass.superclass.getDeclaredField("connection")
      connectionField.trySetAccessible()
      val connection: DuplexConnection = connectionField.get(socket) as DuplexConnection
      val remoteAddress = connection.remoteAddress()
      if (remoteAddress is InetSocketAddress) {
        return "${remoteAddress.hostString}:${remoteAddress.port}"
      }
    } catch (e: Exception) {
      logger.debug("Can't get remote address from ${socket::class.qualifiedName}", e)
      // ignore
    }
    return null
  }
}
