package play.example.game.app.module.playertask.handler

import org.springframework.stereotype.Component
import play.example.game.app.module.player.PlayerService
import play.example.game.app.module.player.Self
import play.example.game.app.module.playertask.domain.PlayerTaskTargetType
import play.example.game.app.module.task.domain.TaskTargetType
import play.example.game.app.module.task.event.PlayerLevelTaskEvent
import play.example.game.app.module.task.res.AbstractTaskResource
import play.example.game.app.module.task.target.PlayerLevelTaskTarget

/**
 * 玩家等级目标处理器
 *
 * @author LiangZengle
 */
@Component
class PlayerLevelTargetHandler(private val playerService: PlayerService) :
  PlayerTaskTargetHandler<PlayerLevelTaskTarget, PlayerLevelTaskEvent>(PlayerTaskTargetType.PlayerLevel) {
  override fun getInitialProgress(owner: Self, target: PlayerLevelTaskTarget, taskConfig: AbstractTaskResource): Int {
    return playerService.getPlayerLevel(owner.id)
  }

  override fun onEvent(
    owner: Self,
    target: PlayerLevelTaskTarget,
    event: PlayerLevelTaskEvent,
    currentProgress: Int,
    taskConfig: AbstractTaskResource
  ): Int {
    return event.currentLv - currentProgress
  }
}
