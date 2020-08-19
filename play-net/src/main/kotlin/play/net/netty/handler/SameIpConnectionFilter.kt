package play.net.netty.handler

import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.ipfilter.AbstractRemoteAddressFilter
import play.util.concurrent.BoundedIntAdder
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentHashMap
import java.util.function.IntSupplier

/**
 * 同ip连接数限制
 *
 * @author LiangZengle
 */
@ChannelHandler.Sharable
open class SameIpConnectionFilter(private val connectionsPerIp: IntSupplier) :
  AbstractRemoteAddressFilter<InetSocketAddress>() {
  constructor(connectionsPerIp: Int) : this(IntSupplier { connectionsPerIp })

  private val counterMap = ConcurrentHashMap<InetAddress, BoundedIntAdder>()

  override fun accept(ctx: ChannelHandlerContext, remoteAddress: InetSocketAddress): Boolean {
    val remoteIp = remoteAddress.address
    val adder = counterMap.computeIfAbsent(remoteIp) { BoundedIntAdder() }
    if (adder.add(1, connectionsPerIp.asInt) < 0) {
      return false
    }
    ctx.channel().closeFuture().addListener {
      adder.add(-1, connectionsPerIp.asInt)
      if (adder.get() < 1) {
        counterMap.computeIfPresent(remoteIp) { _, v ->
          if (v.get() < 1) {
            null
          } else {
            v
          }
        }
      }
    }
    return true
  }
}
