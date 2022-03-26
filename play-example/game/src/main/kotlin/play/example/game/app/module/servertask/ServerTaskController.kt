package play.example.game.app.module.servertask

import org.springframework.beans.factory.`annotation`.Autowired
import org.springframework.stereotype.Component
import play.example.game.app.module.ModuleId
import play.example.game.app.module.player.PlayerManager.Self
import play.mvc.AbstractController
import play.mvc.Cmd
import play.mvc.Controller
import play.mvc.toRequestResult

/**
 * 全服任务模块请求处理
 */
@Component
@Controller(ModuleId.ServerTask)
public class ServerTaskController @Autowired constructor(
  private val playerServerTaskService: PlayerServerTaskService
) : AbstractController(ModuleId.ServerTask) {


  /**
   * 请求领取任务奖励
   *
   * @param self 玩家自己
   * @param taskId 任务id
   */
  @Cmd(1)
  fun reward(self: Self, taskId: Int) = playerServerTaskService.receiveReward(self, taskId).toRequestResult()
}
