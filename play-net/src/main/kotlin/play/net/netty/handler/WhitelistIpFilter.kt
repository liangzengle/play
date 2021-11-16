package play.net.netty.handler

import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.ipfilter.AbstractRemoteAddressFilter
import java.net.InetSocketAddress

/**
 * 白名单
 * @author LiangZengle
 */
@ChannelHandler.Sharable
class WhitelistIpFilter(private val whitelist: Whitelist) : AbstractRemoteAddressFilter<InetSocketAddress>() {
  override fun accept(ctx: ChannelHandlerContext, remoteAddress: InetSocketAddress): Boolean {
    return whitelist.contains(remoteAddress)
  }
}


fun interface Whitelist {
  fun contains(address: InetSocketAddress): Boolean
}
