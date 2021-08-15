package play.example.robot.module.item

import org.springframework.stereotype.Component
import play.example.robot.module.EquipModule
import play.example.robot.module.ItemModule
import play.example.robot.module.player.RobotPlayer

/**
 *
 * @author LiangZengle
 */
@Component
class ItemModuleImpl : ItemModule(){
  override fun useItem2Resp(player: RobotPlayer, statusCode: Int, data: Unit, req: UseItem2RequestParams?) {
    TODO("Not yet implemented")
  }

  override fun useItem1Resp(player: RobotPlayer, statusCode: Int, data: Unit, req: UseItem1RequestParams?) {
    TODO("Not yet implemented")
  }

}
