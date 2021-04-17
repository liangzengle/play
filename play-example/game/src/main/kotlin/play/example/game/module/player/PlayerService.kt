package play.example.game.module.player

import com.google.common.eventbus.Subscribe
import javax.inject.Inject
import javax.inject.Singleton
import play.ApplicationEventListener
import play.Log
import play.example.game.module.account.message.LoginParams
import play.example.game.module.player.entity.PlayerEntity
import play.example.game.module.player.entity.PlayerEntityCache
import play.example.game.module.player.entity.PlayerInfoEntityCache
import play.example.game.module.player.event.*
import play.example.game.module.player.message.PlayerDTO
import play.example.game.module.server.event.ApplicationStartedEvent
import play.example.game.module.server.event.ServerOpenEvent
import play.util.time.currentMillis
import play.util.time.isCurrentMonth
import play.util.time.isCurrentWeek
import play.util.time.isToday

@Singleton
class PlayerService @Inject constructor(
  private val eventBus: PlayerEventBus,
  private val playerCache: PlayerEntityCache,
  private val playerInfoCache: PlayerInfoEntityCache,
  private val onlinePlayerService: OnlinePlayerService
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

  fun login(self: Self, loginParams: LoginParams): PlayerDTO {
    val player = playerCache.getOrThrow(self.id)
    val playerInfo = playerInfoCache.getOrThrow(self.id)
    onlinePlayerService.login(self, loginParams)
    println("player login: $self")
    return PlayerDTO(self.id, playerInfo.name)
  }

  fun createPlayer(id: Long, name: String) {
    println("create player: $name")
    val now = currentMillis()
    val player = PlayerEntity(id, now)
    val playerInfo = play.example.game.module.player.entity.PlayerInfoEntity(id, name)
    playerInfo.lastLoginTime = now
    playerInfo.lastLogoutTime = now
    playerCache.create(player)
    playerInfoCache.create(playerInfo)
  }

  fun isOnline(playerId: Long) = onlinePlayerService.isOnline(playerId)

  fun isPlayerExists(playerId: Long) = PlayerManager.isPlayerExists(playerId)

  fun getPlayerName(playerId: Long) = PlayerManager.getPlayerNameOrThrow(playerId)

  @Subscribe
  private fun onServerOpen(event: ServerOpenEvent) {
    println(Thread.currentThread().toString() + ": server open")
    throw IllegalStateException()
  }

  @Subscribe
  private fun onApplicationStarted(event: ApplicationStartedEvent) {
    Log.info { "testing receive event: $event" }
  }

  /**
   * 判断玩家的创角时间是否在指定的范围内
   *
   * @param self 玩家自己
   * @param from 起始时间（毫秒），0表示不限
   * @param to 截止时间（毫秒），0表示不限
   */
  fun isCreateTimeInRange(self: Self, from: Long, to: Long): Boolean {
    val entity = playerCache.getOrThrow(self.id)
    if (from > 0 && entity.ctime < from) {
      return false
    }
    if (to > 0 && entity.ctime > to) {
      return false
    }
    return true
  }

  fun getPlayerLevel(id: Long): Int {
    TODO("Not yet implemented")
  }
}
