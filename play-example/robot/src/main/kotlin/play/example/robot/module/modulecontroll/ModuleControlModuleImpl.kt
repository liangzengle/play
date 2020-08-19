package play.example.robot.module.modulecontroll

import org.springframework.stereotype.Component
import play.example.robot.module.ModuleControlModule
import play.example.robot.module.player.RobotPlayer

/**
 *
 * @author LiangZengle
 */
@Component
class ModuleControlModuleImpl : ModuleControlModule() {
  override fun moduleOpenResp(player: RobotPlayer, data: Int, req: Any?) {
    TODO("Not yet implemented")
  }

}
