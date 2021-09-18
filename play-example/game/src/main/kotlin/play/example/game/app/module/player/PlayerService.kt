package play.example.game.app.module.player

import mu.KLogging
import play.example.game.app.module.account.message.LoginParams
import play.example.game.app.module.player.entity.PlayerEntity
import play.example.game.app.module.player.entity.PlayerEntityCache
import play.example.game.app.module.player.entity.PlayerInfoEntity
import play.example.game.app.module.player.entity.PlayerInfoEntityCache
import play.example.game.app.module.player.event.*
import play.example.game.app.module.player.message.PlayerDTO
import play.example.game.app.module.player.scheduling.PlayerScheduler
import play.example.game.app.module.reward.model.Cost
import play.example.game.app.module.reward.model.CostResultSet
import play.util.concurrent.PlayFuture
import play.util.concurrent.PlayPromise
import play.util.control.Result2
import play.util.time.Time.currentMillis
import play.util.time.Time.isCurrentMonth
import play.util.time.Time.isCurrentWeek
import play.util.time.Time.isToday
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
@Named
class PlayerService @Inject constructor(
  private val eventBus: PlayerEventBus,
  private val playerCache: PlayerEntityCache,
  private val playerInfoCache: PlayerInfoEntityCache,
  private val onlinePlayerService: OnlinePlayerService,
  private val playerIdNameCache: PlayerIdNameCache,
  private val playerScheduler: PlayerScheduler
) : PlayerEventListener {

  companion object : KLogging()

  override fun playerEventReceive(): PlayerEventReceive {
    return PlayerEventReceiveBuilder()
      .match<PlayerPreLoginEvent>(::preLogin)
      .build()
  }

  fun onNewDayStart(self: Self) {
    val player = playerCache.getOrThrow(self.id)
    if (!isToday(player.dtime)) {
      player.dtime = currentMillis()
      eventBus.postSync(self, PlayerNewDayStartEvent(self.id))
    }
    if (!isCurrentWeek(player.wtime)) {
      player.wtime = currentMillis()
      eventBus.postSync(self, PlayerNewWeekStartEvent(self.id))
    }
    if (!isCurrentMonth(player.mtime)) {
      player.mtime = currentMillis()
      eventBus.postSync(self, PlayerNewMonthStartEvent(self.id))
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
    logger.info("player login: {}", self)
    return PlayerDTO(self.id, playerInfo.name)
  }

  fun logout(self: Self) {
    playerScheduler.cancelAll(self)
    onlinePlayerService.logout(self)
    logger.info("player logout: {}", self)
  }

  fun createPlayer(id: Long, name: String) {
    logger.info("create player: {}", name)
    val now = currentMillis()
    val player = PlayerEntity(id, now)
    val playerInfo = PlayerInfoEntity(id, name)
    playerInfo.lastLoginTime = now
    playerInfo.lastLogoutTime = now
    playerCache.create(player)
    playerInfoCache.create(playerInfo)
  }

  fun isOnline(playerId: Long) = onlinePlayerService.isOnline(playerId)

  fun isPlayerExists(playerId: Long) = playerIdNameCache.isPlayerExists(playerId)

  fun getPlayerName(playerId: Long): String {
    return if (isPlayerExists(playerId)) playerIdNameCache.getPlayerNameOrThrow(playerId) else ""
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

  fun costAsync(playerId: Long, costs: List<Cost>, source: Int): PlayFuture<Result2<CostResultSet>> {
    val costPromise = PlayPromise.make<Result2<CostResultSet>>()
    val costRequest = PlayerExecCost(playerId, costs, source, costPromise)
    eventBus.post(costRequest)
    return costPromise.future
  }
}
