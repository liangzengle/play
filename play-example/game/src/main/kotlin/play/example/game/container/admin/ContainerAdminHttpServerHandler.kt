package play.example.game.container.admin

import io.netty.handler.codec.http.QueryStringDecoder
import org.slf4j.Logger
import play.net.http.Action
import play.net.http.HttpRequestFilter
import play.net.netty.http.BasicNettyHttpRequest
import play.net.netty.http.NettyHttpServerHandler
import play.util.logging.getLogger
import play.util.unsafeCast

/**
 *
 * @author LiangZengle
 */
class ContainerAdminHttpServerHandler(actionManager: ContainerAdminHttpActionManager) :
  NettyHttpServerHandler(actionManager) {
  override val filters: List<HttpRequestFilter> = listOf(AdminWhitelistIpFilter())
  override val logger: Logger = getLogger()

  override fun findAction(request: BasicNettyHttpRequest): Action? {
    val action = actionManager.findAction(request.path())
    if (action != null) {
      return action
    }
    val serverId = QueryStringDecoder(request.toNetty.uri()).parameters()["serverId"]?.get(0)?.toInt() ?: 0
    return actionManager.unsafeCast<ContainerAdminHttpActionManager>()
      .findAction(serverId, request.path(), request.toNetty.uri())
  }
}
