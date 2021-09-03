package play.example.robot.module.mail

import org.springframework.stereotype.Component
import play.example.robot.module.MailModule
import play.example.robot.module.player.RobotPlayer

/**
 *
 * @author LiangZengle
 */
@Component
class MailModuleImpl : MailModule() {
  override fun newMailResp(player: RobotPlayer, data: Int, req: Any?) {
    TODO("Not yet implemented")
  }


  override fun forceDeleteTrashMailsResp(player: RobotPlayer, data: Int, req: Any?) {
    TODO("Not yet implemented")
  }
}
