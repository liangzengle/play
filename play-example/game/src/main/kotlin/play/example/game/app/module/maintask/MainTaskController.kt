package play.example.game.app.module.maintask

import org.springframework.stereotype.Component
import play.example.game.app.module.ModuleId
import play.example.game.app.module.player.PlayerManager.Self
import play.example.module.task.message.TaskProto
import play.mvc.AbstractController
import play.mvc.Cmd
import play.mvc.Controller
import play.mvc.RequestResult
import play.util.control.map

/**
 * 主线任务模块请求处理
 */
@Component
@Controller(ModuleId.MainTask)
public class MainTaskController(
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
  fun accept(self: Self, taskId: Int): RequestResult<TaskProto> = RequestResult {
    mainTaskService.acceptTask(self, taskId).map(mainTaskService::toMessage)
  }

}
