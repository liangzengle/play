package play.example.game.module.mail.event

import play.example.game.module.mail.entity.MailBuilder
import play.example.game.module.player.event.PlayerEvent

data class PlayerMailEvent(override val playerId: Long, val mailBuilder: MailBuilder) : PlayerEvent
