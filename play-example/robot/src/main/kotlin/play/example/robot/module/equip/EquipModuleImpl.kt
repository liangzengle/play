package play.example.robot.module.equip

import org.springframework.stereotype.Component
import play.example.robot.module.EquipModule
import play.example.robot.module.player.RobotPlayer

/**
 *
 * @author LiangZengle
 */
@Component
class EquipModuleImpl : EquipModule() {
  override fun putOnResp(player: RobotPlayer, data: Any, req: PutOnRequestParams?) {
    TODO("Not yet implemented")
  }
}
