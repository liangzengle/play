package play.example.robot.module.item

import org.springframework.stereotype.Component
import play.example.robot.module.ItemModule
import play.example.robot.module.player.RobotPlayer

/**
 *
 * @author LiangZengle
 */
@Component
class ItemModuleImpl : ItemModule() {
  override fun useItem2Resp(player: RobotPlayer, data: Any, req: UseItem2RequestParams?) {
    TODO("Not yet implemented")
  }

  override fun useItem1Resp(player: RobotPlayer, data: Any, req: UseItem1RequestParams?) {
    TODO("Not yet implemented")
  }

}
