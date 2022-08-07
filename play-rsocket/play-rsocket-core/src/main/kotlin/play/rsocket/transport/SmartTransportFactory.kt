package play.rsocket.transport

import io.rsocket.transport.ClientTransport
import io.rsocket.transport.ServerTransport
import java.net.URI

/**
 *
 *
 * @author LiangZengle
 */
class SmartTransportFactory(private val factories: List<TransportFactory>) {

  fun buildClient(uri: String): ClientTransport {
    return buildClient(URI.create(uri))
  }

  fun buildServer(uri: String): ServerTransport<*> {
    return buildServer(URI.create(uri))
  }

  fun acceptScheme(scheme: String): Boolean {
    return factories.any { it.acceptScheme(scheme) }
  }

  fun buildClient(uri: URI): ClientTransport {
    return factories.find { it.acceptScheme(uri.scheme) }?.buildClient(uri)
      ?: throw IllegalArgumentException("Unsupported scheme: $uri")
  }

  fun buildServer(uri: URI): ServerTransport<*> {
    return factories.find { it.acceptScheme(uri.scheme) }?.buildServer(uri)
      ?: throw IllegalArgumentException("Unsupported scheme: $uri")
  }
}
