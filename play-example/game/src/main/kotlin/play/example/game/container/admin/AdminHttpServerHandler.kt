package play.example.game.container.admin

import org.slf4j.Logger
import play.example.game.app.admin.AdminHttpActionManager
import play.net.http.HttpRequestFilter
import play.net.netty.http.NettyHttpServerHandler
import play.util.logging.getLogger

class AdminHttpServerHandler constructor(actionManager: AdminHttpActionManager) :
  NettyHttpServerHandler(actionManager) {
  override val filters: List<HttpRequestFilter> = emptyList()
  override val logger: Logger = getLogger()
}
