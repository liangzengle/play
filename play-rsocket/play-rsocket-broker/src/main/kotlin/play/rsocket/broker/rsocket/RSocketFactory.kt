package play.rsocket.broker.rsocket

import io.rsocket.RSocket

/**
 *
 * @author LiangZengle
 */
fun interface RSocketFactory {
  fun create(): RSocket
}
