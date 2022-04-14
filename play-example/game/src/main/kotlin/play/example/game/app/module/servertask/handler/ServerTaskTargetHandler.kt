package play.example.game.app.module.servertask.handler

import play.example.game.app.module.servertask.domain.ServerTaskTargetType
import play.example.game.app.module.servertask.entity.ServerTaskEntity
import play.example.game.app.module.servertask.event.ServerTaskEvent
import play.example.game.app.module.task.handler.DomainTaskTargetHandler
import play.example.game.app.module.task.target.TaskTarget

abstract class ServerTaskTargetHandler<Target : TaskTarget, Event : ServerTaskEvent>(type: ServerTaskTargetType) :
  DomainTaskTargetHandler<ServerTaskEntity, Target, Event>(type) {
}
