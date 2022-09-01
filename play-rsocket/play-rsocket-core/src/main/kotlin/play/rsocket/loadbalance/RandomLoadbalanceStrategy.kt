package play.rsocket.loadbalance

import io.rsocket.RSocket
import io.rsocket.loadbalance.LoadbalanceStrategy
import java.util.concurrent.ThreadLocalRandom

/**
 *
 *
 * @author LiangZengle
 */
class RandomLoadbalanceStrategy : LoadbalanceStrategy {

  override fun select(sockets: List<RSocket>): RSocket {
    val size = sockets.size
    if (size == 1) {
      return sockets[0]
    }
    val index = ThreadLocalRandom.current().nextInt(size)
    return sockets[index]
  }
}
