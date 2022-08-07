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
    val index = ThreadLocalRandom.current().nextInt(sockets.size)
    return sockets[index]
  }
}
