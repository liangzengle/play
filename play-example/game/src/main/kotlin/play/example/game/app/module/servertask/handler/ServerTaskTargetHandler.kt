package play.example.game.app.module.servertask.handler

import play.example.game.app.module.servertask.entity.ServerTaskEntity
import play.example.game.app.module.task.domain.TaskTargetType
import play.example.game.app.module.task.event.TaskEvent
import play.example.game.app.module.task.handler.DomainTaskTargetHandler
import play.example.game.app.module.task.target.TaskTarget

abstract class ServerTaskTargetHandler<Target : TaskTarget, Event : TaskEvent>(type: TaskTargetType) :
  DomainTaskTargetHandler<ServerTaskEntity, Target, Event>(type) {
}
