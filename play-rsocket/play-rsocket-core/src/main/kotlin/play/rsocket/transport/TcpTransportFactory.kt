package play.rsocket.transport

import io.rsocket.frame.FrameLengthCodec
import io.rsocket.transport.ClientTransport
import io.rsocket.transport.ServerTransport
import io.rsocket.transport.netty.client.TcpClientTransport
import io.rsocket.transport.netty.server.TcpServerTransport
import reactor.netty.tcp.TcpClient
import reactor.netty.tcp.TcpServer
import java.net.URI
import java.util.function.Function

/**
 *
 *
 * @author LiangZengle
 */
class TcpTransportFactory(
  private val maxFrameLength: Int = FrameLengthCodec.FRAME_LENGTH_MASK,
  private val serverOperators: Iterable<Function<TcpServer, TcpServer>> = emptyList(),
  private val clientOperators: Iterable<Function<TcpClient, TcpClient>> = emptyList()
) : TransportFactory {

  override fun acceptScheme(scheme: String): Boolean {
    return "tcp" == scheme
  }

  override fun buildClient(uri: URI): ClientTransport {
    val tcpClient = clientOperators.fold(
      TcpClient.create().host(uri.host).port(uri.port)
    ) { client, mapper -> mapper.apply(client) }
    return TcpClientTransport.create(tcpClient, maxFrameLength)
  }

  override fun buildServer(uri: URI): ServerTransport<*> {
    val tcpServer = serverOperators.fold(
      TcpServer.create().host(uri.host).port(uri.port)
    ) { server, mapper -> mapper.apply(server) }
    return TcpServerTransport.create(tcpServer, maxFrameLength)
  }
}
