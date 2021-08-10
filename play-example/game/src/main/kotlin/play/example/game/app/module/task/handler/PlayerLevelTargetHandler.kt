package play.example.game.app.module.task.handler

import play.example.game.app.module.player.PlayerService
import play.example.game.app.module.player.Self
import play.example.game.app.module.task.config.AbstractTaskResource
import play.example.game.app.module.task.domain.TaskTargetType
import play.example.game.app.module.task.event.PlayerLevelTaskEvent
import play.example.game.app.module.task.target.PlayerLevelTaskTarget
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * 玩家等级目标处理器
 *
 * @author LiangZengle
 */
@Singleton
@Named
class PlayerLevelTargetHandler @Inject constructor(private val playerService: PlayerService) :
  TaskTargetHandler<PlayerLevelTaskTarget, PlayerLevelTaskEvent>(TaskTargetType.PlayerLevel) {
  override fun getInitialProgress(self: Self, target: PlayerLevelTaskTarget, taskConfig: AbstractTaskResource): Int {
    return playerService.getPlayerLevel(self.id)
  }

  override fun onEvent(
    self: Self,
    target: PlayerLevelTaskTarget,
    event: PlayerLevelTaskEvent,
    currentProgress: Int,
    taskConfig: AbstractTaskResource
  ): Int {
    return event.currentLv - currentProgress
  }
}
