package play.example.game.app.module.task.event

import play.example.game.app.module.playertask.domain.PlayerTaskTargetType

/**
 * 玩家等级任务事件
 *
 * @property currentLv 当前等级
 * @author LiangZengle
 */
data class PlayerLevelTaskEvent(val currentLv: Int) : TaskEvent(PlayerTaskTargetType.PlayerLevel)
