package play.example.robot.module.maintask

import org.springframework.stereotype.Component
import play.example.game.app.module.playertask.message.TaskInfo
import play.example.robot.module.MainTaskModule
import play.example.robot.module.player.RobotPlayer

/**
 *
 * @author LiangZengle
 */
@Component
class MainTaskModuleImpl : MainTaskModule() {
  override fun acceptResp(player: RobotPlayer, data: TaskInfo, req: AcceptRequestParams?) {
    TODO("Not yet implemented")
  }

}
