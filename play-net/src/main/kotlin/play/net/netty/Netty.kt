@file:JvmName("Netty")

package play.net.netty

import com.google.common.net.HostAndPort
import io.netty.bootstrap.Bootstrap
import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelFactory
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.EventLoopGroup
import io.netty.channel.epoll.Epoll
import io.netty.channel.epoll.EpollEventLoopGroup
import io.netty.channel.epoll.EpollServerSocketChannel
import io.netty.channel.epoll.EpollSocketChannel
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.ServerSocketChannel
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.util.concurrent.DefaultThreadFactory
import java.net.InetSocketAddress
import java.net.SocketAddress

/**
 * Created by LiangZengle on 2020/2/20.
 */
fun SocketAddress.host(): String {
  return if (this is InetSocketAddress) this.hostString else "127.0.0.1"
}

fun SocketAddress.port(): Int {
  return if (this is InetSocketAddress) this.port else -1
}

fun SocketAddress.toHostAndPort(): HostAndPort {
  return if (this is InetSocketAddress) HostAndPort.fromParts(hostString, port)
  else HostAndPort.fromParts("127.0.0.1", -1)
}

fun ByteBuf.toArray(): ByteArray {
  val byteArray = ByteArray(this.readableBytes())
  this.readBytes(byteArray)
  return byteArray
}

fun ServerBootstrap.options(options: Map<ChannelOption<Any>, Any>): ServerBootstrap {
  options.forEach { (key, value) -> this.option(key, value) }
  return this
}

fun ServerBootstrap.childOptions(options: Map<ChannelOption<Any>, Any>): ServerBootstrap {
  options.forEach { (key, value) -> this.childOption(key, value) }
  return this
}

fun ServerBootstrap.options(options: Iterable<Pair<ChannelOption<Any>, Any>>): ServerBootstrap {
  options.forEach { this.option(it.first, it.second) }
  return this
}

fun ServerBootstrap.childOptions(options: Iterable<Pair<ChannelOption<Any>, Any>>): ServerBootstrap {
  options.forEach { this.childOption(it.first, it.second) }
  return this
}

fun ServerBootstrap.channelInitializer(f: (SocketChannel) -> Unit): ServerBootstrap {
  this.childHandler(object : ChannelInitializer<SocketChannel>() {
    override fun initChannel(ch: SocketChannel) {
      f(ch)
    }
  })
  return this
}

fun Bootstrap.channelInitializer(f: (SocketChannel) -> Unit): Bootstrap {
  this.handler(object : ChannelInitializer<SocketChannel>() {
    override fun initChannel(ch: SocketChannel) {
      f(ch)
    }
  })
  return this
}

fun createEventLoopGroup(name: String, nThread: Int = 0, epollPreferred: Boolean = true): EventLoopGroup {
  val threadFactory = DefaultThreadFactory(name)
  return if (epollPreferred && Epoll.isAvailable()) {
    EpollEventLoopGroup(nThread, threadFactory)
  } else {
    NioEventLoopGroup(nThread, threadFactory)
  }
}

fun createServerChannelFactory(epollPreferred: Boolean = true): ChannelFactory<ServerSocketChannel> {
  return ChannelFactory<ServerSocketChannel> {
    if (epollPreferred && Epoll.isAvailable()) {
      EpollServerSocketChannel()
    } else {
      NioServerSocketChannel()
    }
  }
}

fun createChannelFactory(epollPreferred: Boolean = true): ChannelFactory<SocketChannel> {
  return ChannelFactory<SocketChannel> {
    if (epollPreferred && Epoll.isAvailable()) {
      EpollSocketChannel()
    } else {
      NioSocketChannel()
    }
  }
}
