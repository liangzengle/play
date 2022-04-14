package play.example.game.app.module.playertask.event

import play.example.game.app.module.playertask.domain.PlayerTaskTargetType
import play.example.game.app.module.task.event.TaskEvent

abstract class AbstractPlayerTaskEvent(override val type: PlayerTaskTargetType) : TaskEvent
