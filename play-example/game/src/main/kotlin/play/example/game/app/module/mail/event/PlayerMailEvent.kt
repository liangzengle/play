package play.example.game.app.module.mail.event

import play.example.game.app.module.mail.entity.Mail
import play.example.game.app.module.player.event.PlayerEvent

data class PlayerMailEvent(override val playerId: Long, val mail: Mail) : PlayerEvent
