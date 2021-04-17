package play.net.netty

import io.netty.bootstrap.Bootstrap
import io.netty.channel.*
import io.netty.channel.socket.SocketChannel

/**
 *
 * @author LiangZengle
 */
class NettyClientBuilder {
  private var host: String = ""
  private var port: Int = 0
  private val options = hashMapOf<ChannelOption<Any>, Any>()
  private var handler: ChannelHandler? = null
  private var eventLoopGroup: EventLoopGroup? = null
  private var channelFactory: ChannelFactory<out ServerChannel>? = null

  fun host(host: String): NettyClientBuilder {
    this.host = host
    return this
  }

  fun port(port: Int): NettyClientBuilder {
    this.port = port
    return this
  }

  @Suppress("UNCHECKED_CAST")
  fun <T : Any> option(option: ChannelOption<T>, value: T): NettyClientBuilder {
    options[option as ChannelOption<Any>] = value
    return this
  }

  fun handler(handler: ChannelHandler): NettyClientBuilder {
    this.handler = handler
    return this
  }

  fun handler(channelInitializer: (SocketChannel) -> Unit): NettyClientBuilder {
    this.handler = object : ChannelInitializer<SocketChannel>() {
      override fun initChannel(ch: SocketChannel) {
        channelInitializer(ch)
      }
    }
    return this
  }

  fun group(e: EventLoopGroup): NettyClientBuilder {
    this.eventLoopGroup = e
    return this
  }

  fun channelFactory(factory: ChannelFactory<out ServerChannel>): NettyClientBuilder {
    this.channelFactory = factory
    return this
  }

  fun build(name: String): NettyClient {
    check(host.isNotEmpty()) { "`host` is empty." }
    check(port in 1..65535) { "`port` illegal: $port" }
    val b = Bootstrap()
    options.forEach { (option, value) -> b.option(option, value) }
    handler?.also { b.handler(it) }
    b.channelFactory(channelFactory ?: createServerChannelFactory())
    b.group(eventLoopGroup ?: createEventLoopGroup(name))
    return NettyClient(name, host, port, b)
  }
}
