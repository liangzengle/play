package play.example.game.app.module.maintask.controller

import play.example.game.app.module.ModuleId
import play.example.game.app.module.maintask.MainTaskService
import play.example.game.app.module.player.Self
import play.example.game.app.module.task.message.TaskInfo
import play.mvc.AbstractController
import play.mvc.Cmd
import play.mvc.Controller
import play.mvc.RequestResult
import play.util.control.map
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * 主线任务模块请求处理
 */
@Singleton
@Named
@Controller(ModuleId.MainTask)
public class MainTaskController @Inject constructor(
  private val mainTaskService: MainTaskService
) : AbstractController(ModuleId.MainTask) {


  /**
   * 接取任务
   *
   * @param self 自己
   * @param taskId 任务id
   * @return RequestResult<TaskProto>
   */
  @Cmd(1)
  fun accept(self: Self, taskId: Int): RequestResult<TaskInfo> = RequestResult {
    mainTaskService.acceptTask(self, taskId).map(mainTaskService::toMessage)
  }

}
