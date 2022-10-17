package play.example.game.app.module.servertask.handler

import org.springframework.stereotype.Component
import play.example.game.app.module.servertask.domain.ServerTaskTargetType
import play.example.game.app.module.servertask.entity.ServerTaskEntity
import play.example.game.app.module.servertask.event.ServerLoginEvent
import play.example.game.app.module.servertask.target.ServerLoginTarget
import play.example.game.app.module.task.res.AbstractTaskResource

/**
 *
 *
 * @author LiangZengle
 */
@Component
class ServerLoginTaskTargetHandler :
  ServerTaskTargetHandler<ServerLoginTarget, ServerLoginEvent>(ServerTaskTargetType.ServerLogin) {

  override fun getInitialProgress(
    owner: ServerTaskEntity,
    target: ServerLoginTarget,
    taskConfig: AbstractTaskResource
  ): Int {
    return 0
  }

  override fun onEvent(
    owner: ServerTaskEntity,
    target: ServerLoginTarget,
    event: ServerLoginEvent,
    currentProgress: Int,
    taskConfig: AbstractTaskResource
  ): Int {
    return 1
  }
}
