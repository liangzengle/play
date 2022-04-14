@file:JvmName("Netty")

package play.net.netty

import com.google.common.net.HostAndPort
import io.netty.bootstrap.Bootstrap
import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.ByteBuf
import io.netty.channel.*
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
import io.netty.util.concurrent.Future
import play.util.EmptyByteArray
import play.util.concurrent.PlayFuture
import play.util.unsafeCastOrNull
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.util.concurrent.CompletableFuture

/**
 * Created by LiangZengle on 2020/2/20.
 */
fun SocketAddress.getHost(): String {
  return if (this is InetSocketAddress) this.hostString else "127.0.0.1"
}

fun SocketAddress.getPort(): Int {
  return if (this is InetSocketAddress) this.port else -1
}

@Suppress("UnstableApiUsage")
fun SocketAddress.getHostAndPort(): HostAndPort {
  return if (this is InetSocketAddress) HostAndPort.fromParts(hostString, port)
  else HostAndPort.fromParts("127.0.0.1", 0)
}

fun ByteBuf.copyToArray(): ByteArray {
  val len = readableBytes()
  if (len == 0) return EmptyByteArray
  val byteArray = ByteArray(len)
  readBytes(byteArray)
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
  this.childHandler(
    object : ChannelInitializer<SocketChannel>() {
      override fun initChannel(ch: SocketChannel) {
        f(ch)
      }
    }
  )
  return this
}

fun Bootstrap.channelInitializer(f: (SocketChannel) -> Unit): Bootstrap {
  this.handler(
    object : ChannelInitializer<SocketChannel>() {
      override fun initChannel(ch: SocketChannel) {
        f(ch)
      }
    }
  )
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

fun <T> Future<T>.toCompletableFuture(): CompletableFuture<T> {
  if (isDone) {
    return if (isSuccess) CompletableFuture.completedFuture(now) else CompletableFuture.failedFuture(cause())
  }
  val future = CompletableFuture<T>()
  this.addListener {
    if (it.isSuccess) {
      future.complete(it.now.unsafeCastOrNull())
    } else {
      future.completeExceptionally(it.cause())
    }
  }
  return future
}

fun ChannelFuture.toCompletableFuture(): CompletableFuture<Channel> {
  if (isDone) {
    return if (isSuccess) CompletableFuture.completedFuture(channel()) else CompletableFuture.failedFuture(cause())
  }
  val future = CompletableFuture<Channel>()
  this.addListener {
    if (it.isSuccess) {
      future.complete(this.channel())
    } else {
      future.completeExceptionally(it.cause())
    }
  }
  return future
}

fun <T> Future<T>.toPlay(): PlayFuture<T> {
  return PlayFuture(toCompletableFuture())
}

fun ChannelFuture.toPlay(): PlayFuture<Channel> {
  return PlayFuture(toCompletableFuture())
}
