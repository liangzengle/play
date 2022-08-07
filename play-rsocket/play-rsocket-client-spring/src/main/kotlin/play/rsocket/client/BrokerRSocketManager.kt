package play.rsocket.client

import io.rsocket.RSocket

/**
 *
 *
 * @author LiangZengle
 */
interface BrokerRSocketManager {

  fun getRSocket(): RSocket
}
