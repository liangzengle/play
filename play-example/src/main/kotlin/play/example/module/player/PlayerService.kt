package play.example.module.player

import com.google.common.eventbus.Subscribe
import play.ApplicationEventListener
import play.Log
import play.example.module.account.message.LoginProto
import play.example.module.player.entity.Player
import play.example.module.player.entity.PlayerEntityCache
import play.example.module.player.event.*
import play.example.module.player.message.PlayerProto
import play.example.module.server.event.ApplicationStartedEvent
import play.example.module.server.event.ServerOpenEvent
import play.util.time.currentMillis
import play.util.time.isCurrentMonth
import play.util.time.isCurrentWeek
import play.util.time.isToday
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerService @Inject constructor(
  private val eventBus: PlayerEventBus,
  private val playerCache: PlayerEntityCache,
  private val onlinePlayerService: OnlinePlayerService,
) : PlayerEventListener, ApplicationEventListener {

  override fun playerEventReceive(): PlayerEventReceive {
    return PlayerEventReceiveBuilder()
      .match<PlayerPreLoginEvent>(::preLogin)
      .build()
  }

  fun onNewDayStart(self: Self) {
    val player = playerCache.getOrThrow(self.id)
    if (!isToday(player.dtime)) {
      player.dtime = currentMillis()
      eventBus.postBlocking(self, PlayerNewDayStartEvent(self.id))
    }
    if (!isCurrentWeek(player.wtime)) {
      player.wtime = currentMillis()
      eventBus.postBlocking(self, PlayerNewWeekStartEvent(self.id))
    }
    if (!isCurrentMonth(player.mtime)) {
      player.mtime = currentMillis()
      eventBus.postBlocking(self, PlayerNewMonthStartEvent(self.id))
    }
  }

  private fun preLogin(self: Self) {
    // check new day
    onNewDayStart(self)
  }

  fun login(self: Self, loginParams: LoginProto): PlayerProto {
    val player = playerCache.getOrThrow(self.id)
    onlinePlayerService.login(self, loginParams)
    println("player login: $self")
    return PlayerProto(self.id, player.name)
  }

  fun createPlayer(id: Long, name: String) {
    println("create player: $name")
    playerCache.create(Player(id, name, currentMillis()))
  }

  fun isOnline(playerId: Long) = onlinePlayerService.isOnline(playerId)

  fun isPlayerExists(playerId: Long) = PlayerManager.isPlayerExists(playerId)

  fun getPlayerName(playerId: Long) = PlayerManager.getPlayerNameOrThrow(playerId)

  @Subscribe
  private fun onServerOpen(event: ServerOpenEvent) {
    println(Thread.currentThread().toString() + ": server open")
  }

  @Subscribe
  private fun onApplicationStarted(event: ApplicationStartedEvent) {
    Log.info { "testing receive event: $event" }
  }
}
