package play.example.game.app.module.player

import org.jctools.maps.NonBlockingHashMapLong
import play.example.game.app.module.account.message.LoginParams
import play.example.game.app.module.player.domain.OnlinePlayer
import play.example.game.container.net.Session
import play.mvc.Response
import play.scheduling.Scheduler
import play.util.collection.keysIterator
import play.util.time.Time.currentMillis
import java.time.Duration
import java.util.*
import java.util.function.LongConsumer
import java.util.stream.LongStream
import java.util.stream.StreamSupport
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
@Named
class OnlinePlayerService @Inject constructor(private val scheduler: Scheduler) {

  private val onlinePlayers = NonBlockingHashMapLong<OnlinePlayer>()

  private val offlinePlayers = NonBlockingHashMapLong<OnlinePlayer>()

  companion object {
    private val expireAfterLogout = Duration.ofSeconds(30)
  }

  init {
    scheduler.scheduleWithFixedDelay(expireAfterLogout, ::cleanUp)
  }

  fun login(self: Self, loginProto: LoginParams) {
    val prev = offlinePlayers.remove(self.id)
    val onlinePlayer = prev ?: OnlinePlayer(self.id, loginProto)
    onlinePlayer.logoutTime = 0
    onlinePlayers[self.id] = onlinePlayer
  }

  fun logout(self: Self) {
    val playerId = self.id
    val p = onlinePlayers.remove(playerId)
    p.logoutTime = currentMillis()
    // delay remove
    offlinePlayers[playerId] = p
  }

  fun onlineCount() = onlinePlayers.size

  fun isOnline(playerId: Long): Boolean = onlinePlayers.containsKey(playerId)

  fun broadcast(msg: Response) {
    Session.writeAll(onLinePlayerIdIterator(), msg)
  }

  fun onLinePlayerIdStream(): LongStream {
    val it = onLinePlayerIdIterator()
    val splitter = Spliterators.spliteratorUnknownSize(it, 0)
    return StreamSupport.longStream(splitter, false)
  }

  fun onLinePlayerIdIterator(): PrimitiveIterator.OfLong = onlinePlayers.keysIterator()

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
      return p.loginParams
    }
    p = offlinePlayers[playerId]
    return p?.loginParams
  }

  private fun cleanUp() {
    val expireLoginTime = currentMillis() - expireAfterLogout.toMillis()
    offlinePlayers.values.removeIf { it.logoutTime < expireLoginTime }
  }
}
