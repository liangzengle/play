package play.example.robot.module.mail

import org.springframework.stereotype.Component
import play.example.module.mail.message.MailListProto
import play.example.reward.message.RewardResultSetProto
import play.example.robot.module.MailModule
import play.example.robot.module.player.RobotPlayer

/**
 *
 * @author LiangZengle
 */
@Component
class MailModuleImpl : MailModule() {
  override fun reqMailListResp(player: RobotPlayer, data: MailListProto, req: ReqMailListRequestParams?) {
    TODO("Not yet implemented")
  }

  override fun reqMailRewardResp(player: RobotPlayer, data: RewardResultSetProto, req: ReqMailRewardRequestParams?) {
    TODO("Not yet implemented")
  }

  override fun newMailResp(player: RobotPlayer, data: Int, req: Any?) {
    TODO("Not yet implemented")
  }
}
