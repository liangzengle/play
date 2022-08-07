package play.rsocket.transport

import io.rsocket.frame.FrameLengthCodec
import io.rsocket.transport.ClientTransport
import io.rsocket.transport.ServerTransport
import io.rsocket.transport.netty.client.TcpClientTransport
import io.rsocket.transport.netty.server.TcpServerTransport
import reactor.netty.tcp.TcpClient
import reactor.netty.tcp.TcpServer
import java.net.URI

/**
 *
 *
 * @author LiangZengle
 */
class TcpTransportFactory(private val maxFrameLength: Int) : TransportFactory {
  constructor() : this(FrameLengthCodec.FRAME_LENGTH_MASK)

  override fun acceptScheme(scheme: String): Boolean {
    return "tcp" == scheme
  }

  override fun buildClient(uri: URI): ClientTransport {
    val tcpClient = TcpClient.create().host(uri.host).port(uri.port)
    return TcpClientTransport.create(tcpClient, maxFrameLength)
  }

  override fun buildServer(uri: URI): ServerTransport<*> {
    val tcpServer = TcpServer.create().host(uri.host).port(uri.port)
    return TcpServerTransport.create(tcpServer, maxFrameLength)
  }
}
