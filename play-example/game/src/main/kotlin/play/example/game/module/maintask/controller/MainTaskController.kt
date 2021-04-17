package play.example.game.module.maintask.controller

import javax.inject.Inject
import javax.inject.Singleton
import play.example.game.module.ModuleId
import play.example.game.module.maintask.MainTaskService
import play.example.game.module.player.Self
import play.example.game.module.task.message.TaskInfo
import play.mvc.AbstractController
import play.mvc.Cmd
import play.mvc.Controller
import play.mvc.RequestResult
import play.util.control.map

/**
 * 主线任务模块请求处理
 */
@Singleton
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
