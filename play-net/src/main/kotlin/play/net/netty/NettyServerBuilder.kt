package play.net.netty

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.*
import io.netty.channel.socket.ServerSocketChannel
import io.netty.channel.socket.SocketChannel

/**
 *
 * @author LiangZengle
 */
class NettyServerBuilder {
  private var host: String = ""
  private var port: Int = 0
  private val options = hashMapOf<ChannelOption<Any>, Any>()
  private val childOptions = hashMapOf<ChannelOption<Any>, Any>()
  private var handler: ChannelHandler? = null
  private var childHandler: ChannelHandler? = null
  private var parentEventLoopGroup: EventLoopGroup? = null
  private var childEventLoopGroup: EventLoopGroup? = null
  private var channelFactory: ChannelFactory<out ServerChannel>? = null

  fun host(host: String): NettyServerBuilder {
    this.host = host
    return this
  }

  fun port(port: Int): NettyServerBuilder {
    this.port = port
    return this
  }

  @Suppress("UNCHECKED_CAST")
  fun <T : Any> option(option: ChannelOption<T>, value: T): NettyServerBuilder {
    options[option as ChannelOption<Any>] = value
    return this
  }

  @Suppress("UNCHECKED_CAST")
  fun <T : Any> childOption(option: ChannelOption<T>, value: T): NettyServerBuilder {
    childOptions[option as ChannelOption<Any>] = value
    return this
  }

  fun handler(handler: ChannelHandler): NettyServerBuilder {
    this.handler = handler
    return this
  }

  fun handler(channelInitializer: (ServerSocketChannel) -> Unit): NettyServerBuilder {
    this.handler = object : ChannelInitializer<ServerSocketChannel>() {
      override fun initChannel(ch: ServerSocketChannel) {
        channelInitializer(ch)
      }
    }
    return this
  }

  fun childHandler(handler: ChannelHandler): NettyServerBuilder {
    this.childHandler = handler
    return this
  }

  fun childHandler(channelInitializer: (SocketChannel) -> Unit): NettyServerBuilder {
    this.childHandler = object : ChannelInitializer<SocketChannel>() {
      override fun initChannel(ch: SocketChannel) {
        channelInitializer(ch)
      }
    }
    return this
  }

  fun eventLoopGroup(parent: EventLoopGroup, child: EventLoopGroup): NettyServerBuilder {
    this.parentEventLoopGroup = parent
    this.childEventLoopGroup = child
    return this
  }

  fun channelFactory(factory: ChannelFactory<out ServerChannel>): NettyServerBuilder {
    this.channelFactory = factory
    return this
  }

  fun build(name: String): NettyServer {
    require(host.isNotEmpty()) { "`host` is empty." }
    require(port in 1..65535) { "`port` illegal: $port" }
    val b = ServerBootstrap()
    b.options(options)
    b.childOptions(childOptions)
    handler?.also { b.handler(it) }
    childHandler?.also { b.childHandler(it) }
    b.channelFactory(channelFactory ?: createServerChannelFactory())
    b.group(
      parentEventLoopGroup ?: createEventLoopGroup("$name-parent", 1),
      childEventLoopGroup ?: createEventLoopGroup("$name-child")
    )
    return NettyServer(name, host, port, b)
  }
}
