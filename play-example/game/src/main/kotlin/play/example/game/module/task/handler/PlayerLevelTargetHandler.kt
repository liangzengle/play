package play.example.game.module.task.handler

import play.example.game.module.player.PlayerService
import play.example.game.module.player.Self
import play.example.game.module.task.config.AbstractTaskConfig
import play.example.game.module.task.domain.TaskTargetType
import play.example.game.module.task.event.PlayerLevelTaskEvent
import play.example.game.module.task.target.PlayerLevelTaskTarget
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 玩家等级目标处理器
 *
 * @author LiangZengle
 */
@Singleton
class PlayerLevelTargetHandler @Inject constructor(private val playerService: PlayerService) :
  TaskTargetHandler<PlayerLevelTaskTarget, PlayerLevelTaskEvent>(TaskTargetType.PlayerLevel) {
  override fun getInitialProgress(self: Self, target: PlayerLevelTaskTarget, taskConfig: AbstractTaskConfig): Int {
    return playerService.getPlayerLevel(self.id)
  }

  override fun onEvent(
    self: Self,
    target: PlayerLevelTaskTarget,
    event: PlayerLevelTaskEvent,
    currentProgress: Int,
    taskConfig: AbstractTaskConfig
  ): Int {
    return event.currentLv - currentProgress
  }
}
