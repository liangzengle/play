package play.example.module.player

import java.time.Duration
import java.util.*
import java.util.function.LongConsumer
import java.util.stream.LongStream
import java.util.stream.StreamSupport
import javax.inject.Inject
import javax.inject.Singleton
import org.jctools.maps.NonBlockingHashMapLong
import play.example.common.net.SessionActor
import play.example.module.account.message.LoginParams
import play.util.collection.keysIterator
import play.util.scheduling.Scheduler
import play.util.unsafeCast

@Singleton
class OnlinePlayerService @Inject constructor(private val scheduler: Scheduler) {

  private val onlinePlayers = NonBlockingHashMapLong<LoginParams>()

  private val offlinePlayers = NonBlockingHashMapLong<LoginParams>()

  fun login(self: Self, loginProto: LoginParams) {
    onlinePlayers[self.id] = loginProto
    offlinePlayers.remove(self.id)
  }

  fun logout(self: Self) {
    val playerId = self.id
    val p = onlinePlayers.remove(playerId)
    // delay remove
    offlinePlayers[playerId] = p
    scheduler.schedule(Duration.ofSeconds(30)) {
      offlinePlayers.remove(playerId, p.unsafeCast<Any>())
    }
  }

  fun onlineCount() = onlinePlayers.size

  fun isOnline(playerId: Long): Boolean = onlinePlayers.containsKey(playerId)

  fun broadcast(msg: Any) {
    SessionActor.writeAll(onLinePlayerIdIterator(), msg)
  }

  fun onLinePlayerIdStream(): LongStream {
    val it = onLinePlayerIdIterator()
    val splitter = Spliterators.spliteratorUnknownSize(it, 0)
    return StreamSupport.longStream(splitter, false)
  }

  fun onLinePlayerIdIterator() = onlinePlayers.keysIterator()

  fun foreach(action: LongConsumer) {
    val it = onLinePlayerIdIterator()
    it.forEachRemaining(action)
  }

  fun getLoginParams(playerId: Long): Optional<LoginParams> {
    return Optional.ofNullable(getLoginParamsOrNull(playerId))
  }

  fun getLoginParamsOrNull(playerId: Long): LoginParams? {
    var p = onlinePlayers[playerId]
    if (p != null) {
      return p
    }
    p = offlinePlayers[playerId]
    return p
  }
}
