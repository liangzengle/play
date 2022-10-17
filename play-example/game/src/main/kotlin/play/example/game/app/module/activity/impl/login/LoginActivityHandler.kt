package play.example.game.app.module.activity.impl.login

import org.springframework.stereotype.Component
import play.example.common.StatusCode
import play.example.game.app.module.activity.base.ActivityHandler
import play.example.game.app.module.activity.base.ActivityType
import play.example.game.app.module.activity.base.PlayerActivityDataKey
import play.example.game.app.module.activity.base.PlayerActivityService
import play.example.game.app.module.activity.impl.login.data.LoginActivityData
import play.example.game.app.module.activity.impl.login.domain.LoginActivityLogSource
import play.example.game.app.module.activity.impl.login.domain.LoginActivityStatusCode
import play.example.game.app.module.activity.impl.login.res.LoginActivityResourceSet
import play.example.game.app.module.player.PlayerManager
import play.example.game.app.module.player.PlayerServiceFacade
import play.example.game.app.module.player.event.PlayerEventBus
import play.example.game.app.module.player.event.PlayerNewDayStartEvent
import play.example.game.app.module.player.event.subscribe
import play.example.game.app.module.reward.message.toProto
import play.example.reward.message.RewardResultSetProto
import play.util.control.Result2
import play.util.control.map
import play.util.primitive.Bit

/**
 *
 * @author LiangZengle
 */
@Component
class LoginActivityHandler(
  private val serviceFacade: PlayerServiceFacade,
  private val playerActivityService: PlayerActivityService,
  eventBus: PlayerEventBus
) : ActivityHandler {

  init {
    eventBus.subscribe<PlayerNewDayStartEvent>(::onNewDayStart)
  }

  override fun type(): ActivityType = ActivityType.LOGIN

  private fun onNewDayStart(self: PlayerManager.Self) {
    playerActivityService.process(self, ActivityType.LOGIN) { _, playerEntity ->
      val data = playerEntity.data.attr(PlayerActivityDataKey.Login).computeIfAbsent(::LoginActivityData)
      data.days++
      StatusCode.Success
    }
  }

  fun getReward(self: PlayerManager.Self, activityId: Int, day: Int): Result2<RewardResultSetProto> {
    val resource = LoginActivityResourceSet.getOrNull(day) ?: return StatusCode.ResourceNotFound
    return playerActivityService.process(self, activityId) { _, playerEntity ->
      val data = playerEntity.data.attr(PlayerActivityDataKey.Login).computeIfAbsent(::LoginActivityData)
      if (resource.id < data.days) {
        LoginActivityStatusCode.LoginNotEnough
      } else if (Bit.is1(data.flags, day)) {
        LoginActivityStatusCode.RewardReceived
      } else {
        serviceFacade.tryAndExecReward(self, resource.rewards, LoginActivityLogSource.Reward).map {
          data.flags = Bit.set1(data.flags, day)
          it.toProto()
        }
      }
    }
  }
}
