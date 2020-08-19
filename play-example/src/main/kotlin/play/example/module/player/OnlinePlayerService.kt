package play.example.module.player

import org.jctools.maps.NonBlockingHashMapLong
import play.example.common.collection.toJava
import play.example.common.net.SessionActor
import play.example.module.account.message.LoginProto
import play.util.unsafeCast
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.function.LongConsumer
import java.util.stream.LongStream
import java.util.stream.StreamSupport
import javax.inject.Singleton

@Singleton
class OnlinePlayerService {

  private val onlinePlayers = ConcurrentHashMap<Long, OnlinePlayer>() // TODO 离线保留5分钟

  fun login(self: Self, loginProto: LoginProto) {
    onlinePlayers.computeIfAbsent(self.id) { id -> OnlinePlayer(id, loginProto) }
  }

  fun logout(self: Self) {
    onlinePlayers.remove(self.id)
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

  private fun onLinePlayerIdIterator() =
    onlinePlayers.keys().unsafeCast<NonBlockingHashMapLong<OnlinePlayer>.IteratorLong>().toJava()

  fun foreach(action: LongConsumer) {
    val it = onLinePlayerIdIterator()
    it.forEachRemaining(action)
  }
}

class OnlinePlayer(val playerId: Long, val loginParams: LoginProto) {
  override fun equals(other: Any?): Boolean {
    return other is OnlinePlayer && other.playerId == playerId
  }

  override fun hashCode(): Int {
    return playerId.hashCode()
  }

  override fun toString(): String {
    return "OnlinePlayer($playerId)"
  }
}
