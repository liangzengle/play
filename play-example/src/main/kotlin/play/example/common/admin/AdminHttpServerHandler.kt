package play.example.common.admin

import io.netty.channel.ChannelHandler
import org.slf4j.Logger
import play.getLogger
import play.net.http.HttpRequestFilter
import play.net.netty.http.NettyHttpServerHandler
import javax.inject.Inject
import javax.inject.Singleton

@ChannelHandler.Sharable
@Singleton
class AdminHttpServerHandler @Inject constructor(actionManager: AdminHttpActionManager) :
  NettyHttpServerHandler(actionManager) {
  override val filters: List<HttpRequestFilter> = emptyList()
  override val logger: Logger = getLogger()
}
