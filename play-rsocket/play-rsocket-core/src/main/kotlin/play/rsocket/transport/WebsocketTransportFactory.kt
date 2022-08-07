package play.rsocket.transport

import io.rsocket.transport.ClientTransport
import io.rsocket.transport.ServerTransport
import io.rsocket.transport.netty.client.WebsocketClientTransport
import io.rsocket.transport.netty.server.WebsocketServerTransport
import java.net.URI

/**
 *
 *
 * @author LiangZengle
 */
class WebsocketTransportFactory : TransportFactory {

  override fun acceptScheme(scheme: String): Boolean {
    return "ws" == scheme
  }

  override fun buildClient(uri: URI): ClientTransport {
    return WebsocketClientTransport.create(uri)
  }

  override fun buildServer(uri: URI): ServerTransport<*> {
    return WebsocketServerTransport.create(uri.host, uri.port)
  }
}
