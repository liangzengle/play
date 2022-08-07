package play.rsocket.transport

import io.rsocket.transport.ClientTransport
import io.rsocket.transport.ServerTransport
import java.net.URI

/**
 *
 *
 * @author LiangZengle
 */
interface TransportFactory {

  fun acceptScheme(scheme: String): Boolean

  fun buildClient(uri: URI): ClientTransport

  fun buildServer(uri: URI): ServerTransport<*>
}
