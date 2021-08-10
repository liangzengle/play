package play.example.game.app.module.mail.event

import play.example.game.app.module.mail.entity.MailBuilder
import play.example.game.app.module.player.event.PlayerEvent

data class PlayerMailEvent(override val playerId: Long, val mailBuilder: MailBuilder) : PlayerEvent
