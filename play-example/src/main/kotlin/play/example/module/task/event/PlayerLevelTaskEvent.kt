package play.example.module.task.event

import play.example.module.task.domain.TaskTargetType

/**
 * 玩家等级任务事件
 *
 * @property currentLv 当前等级
 * @author LiangZengle
 */
data class PlayerLevelTaskEvent(val currentLv: Int) : TaskEvent(TaskTargetType.PlayerLevel)
