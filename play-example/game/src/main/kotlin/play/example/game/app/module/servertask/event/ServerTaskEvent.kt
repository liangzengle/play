package play.example.game.app.module.servertask.event

import play.example.game.app.module.servertask.ServerTaskManager
import play.example.game.app.module.servertask.domain.ServerTaskTargetType
import play.example.game.app.module.task.event.TaskEvent

abstract class ServerTaskEvent(override val targetType: ServerTaskTargetType) : TaskEvent, ServerTaskManager.Command
