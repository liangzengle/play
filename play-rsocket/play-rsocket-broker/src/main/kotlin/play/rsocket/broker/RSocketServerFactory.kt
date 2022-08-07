package play.rsocket.broker

import io.rsocket.core.RSocketServer

/**
 *
 *
 * @author LiangZengle
 */
interface RSocketServerFactory {
  fun create(): RSocketServer
}
