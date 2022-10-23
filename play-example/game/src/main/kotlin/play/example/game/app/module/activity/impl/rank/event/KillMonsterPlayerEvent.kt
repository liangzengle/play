package play.example.game.app.module.activity.impl.rank.event

import play.example.game.app.module.player.event.PlayerEvent

/**
 *
 * @author LiangZengle
 */
data class KillMonsterPlayerEvent(override val playerId: Long, val num: Int) : PlayerEvent
