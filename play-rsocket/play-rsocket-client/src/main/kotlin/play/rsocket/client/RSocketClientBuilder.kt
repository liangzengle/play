package play.rsocket.client

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.Unpooled
import io.rsocket.SocketAcceptor
import io.rsocket.core.RSocketClient
import io.rsocket.core.RSocketConnector
import io.rsocket.frame.decoder.PayloadDecoder
import io.rsocket.metadata.CompositeMetadataCodec
import io.rsocket.metadata.WellKnownMimeType
import io.rsocket.transport.ClientTransport
import io.rsocket.util.ByteBufPayload
import play.rsocket.metadata.MimeTypes
import play.rsocket.metadata.RouteSetupMetadata

/**
 *
 *
 * @author LiangZengle
 */
class RSocketClientBuilder {
  private var id: Int = 0
  private var role: Byte = 0
  private var setupData: ByteBuf = Unpooled.EMPTY_BUFFER
  private var setupMetadatas: MutableList<Pair<String, ByteBuf>> = arrayListOf()
  private var allocator: ByteBufAllocator = ByteBufAllocator.DEFAULT
  private var connector: RSocketConnector = RSocketConnector.create().payloadDecoder(PayloadDecoder.ZERO_COPY)
    .metadataMimeType(WellKnownMimeType.MESSAGE_RSOCKET_COMPOSITE_METADATA.toString())
  private var acceptor: SocketAcceptor? = null
  private lateinit var uri: String
  private lateinit var transport: ClientTransport

  fun id(id: Int): RSocketClientBuilder {
    this.id = id
    return this
  }

  fun role(role: Byte): RSocketClientBuilder {
    this.role = role
    return this
  }

  fun setupData(setupData: ByteBuf): RSocketClientBuilder {
    this.setupData = setupData
    return this
  }

  fun setupMetadata(name: String, data: ByteBuf): RSocketClientBuilder {
    this.setupMetadatas.add(name to data)
    return this
  }

  fun allocator(allocator: ByteBufAllocator): RSocketClientBuilder {
    this.allocator = allocator
    return this
  }

  fun connector(connector: RSocketConnector): RSocketClientBuilder {
    this.connector = connector
    return this
  }

  fun customizeConnector(customizer: (RSocketConnector) -> Unit): RSocketClientBuilder {
    customizer(this.connector)
    return this
  }

  fun acceptor(socketAcceptor: SocketAcceptor): RSocketClientBuilder {
    this.acceptor = socketAcceptor
    return this
  }

  fun transport(address: String, transport: ClientTransport): RSocketClientBuilder {
    this.uri = address
    this.transport = transport
    return this
  }

  fun transport(uri: String, factory: (String) -> ClientTransport): RSocketClientBuilder {
    this.uri = uri
    this.transport = factory(uri)
    return this
  }

  fun build(): RSocketClient {
    val setupMetadata = allocator.compositeBuffer()

    val routeSetup = RouteSetupMetadata(id, role)
    CompositeMetadataCodec.encodeAndAddMetadata(
      setupMetadata, allocator, MimeTypes.RouteSetup, routeSetup.content
    )
    setupMetadatas.forEach { (name, data) ->
      CompositeMetadataCodec.encodeAndAddMetadata(setupMetadata, allocator, name, data)
    }

    val setupPayload = ByteBufPayload.create(setupData, setupMetadata)
    connector.setupPayload(setupPayload)

    acceptor?.also(connector::acceptor)

    return RSocketClient.from(connector.connect(transport))
  }
}
