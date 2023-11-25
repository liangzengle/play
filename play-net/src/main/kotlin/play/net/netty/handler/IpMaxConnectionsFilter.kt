package play.net.netty.handler

import com.github.benmanes.caffeine.cache.Caffeine
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.ipfilter.AbstractRemoteAddressFilter
import io.netty.util.AttributeKey
import play.util.concurrent.BoundedIntAdder
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.function.IntSupplier

/**
 * 同ip连接数限制
 *
 * @author LiangZengle
 */
@ChannelHandler.Sharable
open class IpMaxConnectionsFilter(private val connectionsPerIp: IntSupplier, private val whitelist: Whitelist) :
  AbstractRemoteAddressFilter<InetSocketAddress>() {
  constructor(connectionsPerIp: Int, whitelist: Whitelist) : this(IntSupplier { connectionsPerIp }, whitelist)

  companion object {
    private val ATTR_COUNTER = AttributeKey.valueOf<BoundedIntAdder>("IP_CONNECTIONS_COUNTER")
  }

  private val counterMap = Caffeine.newBuilder().weakValues().build<InetAddress, BoundedIntAdder> { BoundedIntAdder() }

  override fun accept(ctx: ChannelHandlerContext, remoteAddress: InetSocketAddress): Boolean {
    if (whitelist.contains(remoteAddress)) {
      return true
    }
    val remoteIp = remoteAddress.address
    val adder = counterMap.get(remoteIp)
    if (adder.add(1, connectionsPerIp.asInt) == 0) {
      return false
    }
    ctx.channel().attr(ATTR_COUNTER).set(adder)
    return true
  }

  override fun channelInactive(ctx: ChannelHandlerContext) {
    super.channelInactive(ctx)
    if (ctx.channel().hasAttr(ATTR_COUNTER)) {
      ctx.channel().attr(ATTR_COUNTER).get()?.decrease()
    }
  }
}
