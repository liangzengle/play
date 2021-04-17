package play.example.game.base.admin

import javax.inject.Inject
import org.slf4j.Logger
import play.net.http.HttpRequestFilter
import play.net.netty.http.NettyHttpServerHandler
import play.util.logging.getLogger

class AdminHttpServerHandler @Inject constructor(actionManager: AdminHttpActionManager) :
  NettyHttpServerHandler(actionManager) {
  override val filters: List<HttpRequestFilter> = emptyList()
  override val logger: Logger = getLogger()
}
