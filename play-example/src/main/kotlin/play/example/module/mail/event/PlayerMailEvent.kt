package play.example.module.mail.event

import play.example.module.mail.entity.MailBuilder
import play.example.module.player.event.PlayerEvent

data class PlayerMailEvent(override val playerId: Long, val mailBuilder: MailBuilder) : PlayerEvent
