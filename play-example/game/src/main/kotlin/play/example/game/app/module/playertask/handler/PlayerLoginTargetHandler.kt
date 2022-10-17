package play.example.game.app.module.playertask.handler

import org.springframework.stereotype.Component
import play.example.game.app.module.player.PlayerManager
import play.example.game.app.module.player.PlayerService
import play.example.game.app.module.player.event.PlayerDayFirstLoginEvent
import play.example.game.app.module.playertask.domain.PlayerTaskTargetType
import play.example.game.app.module.playertask.target.PlayerLoginTaskTarget
import play.example.game.app.module.task.res.AbstractTaskResource

/**
 *
 *
 * @author LiangZengle
 */
@Component
class PlayerLoginTargetHandler(private val playerService: PlayerService) :
  PlayerTaskTargetHandler<PlayerLoginTaskTarget, PlayerDayFirstLoginEvent>(PlayerTaskTargetType.PlayerLogin) {

  override fun getInitialProgress(
    owner: PlayerManager.Self,
    target: PlayerLoginTaskTarget,
    taskConfig: AbstractTaskResource
  ): Int {
    return if (playerService.todayHasLogin(owner.id)) 1 else 0
  }

  override fun onEvent(
    owner: PlayerManager.Self,
    target: PlayerLoginTaskTarget,
    event: PlayerDayFirstLoginEvent,
    currentProgress: Int,
    taskConfig: AbstractTaskResource
  ): Int {
    return 1
  }
}
