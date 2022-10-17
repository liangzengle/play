package play.example.game.app.module.player

import mu.KLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import play.example.common.StatusCode
import play.example.game.app.module.item.res.ItemResourceSet
import play.example.game.app.module.player.PlayerManager.Self
import play.example.game.app.module.player.domain.PlayerErrorCode
import play.example.game.app.module.player.entity.PlayerEntity
import play.example.game.app.module.player.entity.PlayerEntityCache
import play.example.game.app.module.player.entity.PlayerInfoEntity
import play.example.game.app.module.player.entity.PlayerInfoEntityCache
import play.example.game.app.module.player.event.*
import play.example.game.app.module.player.scheduling.PlayerScheduler
import play.example.game.app.module.reward.RewardService
import play.example.game.app.module.reward.message.toProto
import play.example.game.app.module.reward.model.CostList
import play.example.game.app.module.reward.model.CostResultSet
import play.example.game.container.net.Session
import play.example.module.login.message.LoginParams
import play.example.player.message.PlayerProto
import play.util.concurrent.PlayFuture
import play.util.concurrent.PlayPromise
import play.util.concurrent.Promise
import play.util.control.Result2
import play.util.control.ok
import play.util.control.peek
import play.util.getOrNull
import play.util.time.Time

@Component
class PlayerService(
  private val eventBus: PlayerEventBus,
  private val playerCache: PlayerEntityCache,
  private val playerInfoCache: PlayerInfoEntityCache,
  private val onlinePlayerService: OnlinePlayerService,
  private val playerIdNameCache: PlayerIdNameCache,
  private val playerScheduler: PlayerScheduler,
  @Autowired(required = false)
  private val playerNameChecker: PlayerNameChecker?,
  private val rewardService: RewardService
) {

  companion object : KLogging()

  init {
    eventBus.subscribe<PlayerPreLoginEvent>(::preLogin)
    eventBus.subscribe(::onCheckNameResult)
    eventBus.subscribe(::execCost)
  }

  fun onNewDayStart(self: Self) {
    val player = playerCache.getOrThrow(self.id)
    if (!Time.isToday(player.dtime)) {
      player.dtime = Time.currentMillis()
      eventBus.publishSync(self, PlayerNewDayStartEvent(self.id))
      eventBus.publish(self, PlayerDayFirstLoginEvent(self.id))
    }
    if (!Time.isCurrentWeek(player.wtime)) {
      player.wtime = Time.currentMillis()
      eventBus.publishSync(self, PlayerNewWeekStartEvent(self.id))
    }
    if (!Time.isCurrentMonth(player.mtime)) {
      player.mtime = Time.currentMillis()
      eventBus.publishSync(self, PlayerNewMonthStartEvent(self.id))
    }
  }

  private fun preLogin(self: Self) {
    // check new day
    onNewDayStart(self)
  }

  fun login(self: Self, loginParams: LoginParams): PlayerProto {
    val player = playerCache.getOrThrow(self.id)
    val playerInfo = playerInfoCache.getOrThrow(self.id)
    playerInfo.lastLoginTime = Time.currentMillis()
    if (playerInfo.lastLogoutTime == 0L) {
      playerInfo.lastLogoutTime = playerInfo.lastLoginTime
    }
    onlinePlayerService.login(self, loginParams)
    logger.info("player login: {}", self)
    return PlayerProto(self.id, playerInfo.name)
  }

  fun afterLogin(self: Self) {
    eventBus.publish(PlayerLoginEvent(self.id))
  }

  fun logout(self: Self) {
    playerScheduler.cancelAll(self)
    onlinePlayerService.logout(self)
    playerInfoCache.getOrThrow(self.id).lastLogoutTime = Time.currentMillis()
    logger.info("player logout: {}", self)
  }

  fun createPlayer(id: Long, name: String) {
    logger.info("create player: {}", name)
    val now = Time.currentMillis()
    val player = PlayerEntity(id, now)
    val playerInfo = PlayerInfoEntity(id, name)
    playerCache.create(player)
    playerInfoCache.create(playerInfo)
  }

  fun todayHasLogin(id: Long): Boolean {
    val playerInfo = playerInfoCache.getOrNull(id) ?: return false
    return Time.isToday(playerInfo.lastLoginTime)
  }

  fun isOnline(playerId: Long) = onlinePlayerService.isOnline(playerId)

  fun isPlayerExists(playerId: Long) = playerIdNameCache.isPlayerExists(playerId)

  fun getPlayerNameOrThrow(playerId: Long): String {
    return playerIdNameCache.getPlayerNameOrThrow(playerId)
  }

  fun getPlayerNameOrElse(playerId: Long, defaultValue: String): String {
    return if (isPlayerExists(playerId)) playerIdNameCache.getPlayerNameOrThrow(playerId) else defaultValue
  }

  fun getPlayerNameOrNull(playerId: Long): String? {
    return playerIdNameCache.getPlayerName(playerId).getOrNull()
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

  fun costAsync(playerId: Long, costs: CostList, source: Int): PlayFuture<Result2<CostResultSet>> {
    val costPromise = PlayPromise.make<Result2<CostResultSet>>()
    val costRequest = PlayerExecCost(playerId, costs, source, costPromise)
    eventBus.publish(costRequest)
    return costPromise.future
  }

  private fun execCost(self: Self, event: PlayerExecCost) {
    event.promise.catchingComplete {
      rewardService.tryAndExecCost(self, event.costs, event.logSource).peek {
        Session.write(self.id, PlayerModule.RewardOrCostPush(it.toProto()))
      }
    }
  }

  fun changeName(self: Self, newName: String): PlayFuture<Result2<String>> {
    return if (playerNameChecker == null) {
      PlayFuture.successful(doChangeName(self, newName, ItemResourceSet.extension().changeNameCosts()))
    } else {
      with(eventBus) {
        playerNameChecker.check(newName)
          .pipeToPlayer { result, promise -> CheckNameResult(self.id, newName, result, promise) }
      }
    }
  }

  private fun onCheckNameResult(self: Self, checkResult: CheckNameResult) {
    val promise = checkResult.promise
    if (checkResult.result.isFailure) {
      promise.success(StatusCode.Failure)
    } else if (checkResult.result.getOrNull() != true) {
      promise.success(PlayerErrorCode.PlayerNameNotAvailable)
    } else {
      promise.catchingComplete {
        doChangeName(self, checkResult.newName, ItemResourceSet.extension().changeNameCosts())
      }
    }
  }

  private fun doChangeName(self: Self, newName: String, costs: CostList): Result2<String> {
    val tryResult = rewardService.tryCost(self, costs, 1)
    if (tryResult.isErr()) {
      return tryResult.asErrResult()
    }
    val changeNameResult = playerIdNameCache.changeName(self.id, newName).toResult2()
    if (changeNameResult.isErr()) {
      return changeNameResult.asErrResult()
    }
    val costResult = rewardService.execCost(self, tryResult.get())
    Session.write(self.id, PlayerModule.RewardOrCostPush(costResult.toProto()))
    return ok(newName)
  }

  private class CheckNameResult(
    override val playerId: Long,
    val newName: String,
    val result: Result<Boolean>,
    override val promise: Promise<Result2<String>>
  ) : PromisedPlayerEvent<Result2<String>>

  private data class PlayerExecCost(
    override val playerId: Long,
    val costs: CostList,
    val logSource: Int,
    override val promise: PlayPromise<Result2<CostResultSet>>
  ) : PromisedPlayerEvent<Result2<CostResultSet>>
}
