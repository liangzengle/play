package play.example.game.app.module.mail.event

import play.example.game.app.module.player.event.PlayerEvent

/**
 *
 *
 * @author LiangZengle
 */
data class PlayerCheckMailboxEvent(override val playerId: Long) : PlayerEvent
