package play.example.game.app.module.playertask.event

import play.example.game.app.module.playertask.domain.PlayerTaskTargetType
import play.example.game.app.module.task.domain.TaskTargetType

/**
 * 玩家等级任务事件
 *
 * @property currentLv 当前等级
 * @author LiangZengle
 */
data class PlayerLevelTaskEvent(val currentLv: Int) : IPlayerTaskEvent {
  override val targetType: TaskTargetType
    get() = PlayerTaskTargetType.PlayerLevel
}
