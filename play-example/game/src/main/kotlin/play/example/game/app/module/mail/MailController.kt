package play.example.game.app.module.mail

import org.springframework.stereotype.Component
import play.example.game.app.module.ModuleId
import play.example.game.app.module.player.PlayerManager
import play.example.module.mail.message.MailListProto
import play.example.reward.message.RewardResultSetProto
import play.mvc.*


@Controller(ModuleId.Mail)
@Component
class MailController(private val service: MailService) : AbstractController(ModuleId.Mail) {

  /**
   * 请求邮件列表
   * @param self 玩家自己
   * @param start 第几封开始（从0开始）
   * @param count 数量
   * @return MailListProto
   */
  @Cmd(1)
  fun reqMailList(self: PlayerManager.Self, start: Int, count: Int): RequestResult<MailListProto> =
    RequestResult.ok {
      service.reqMailList(self, start, count)
    }

  /**
   * 请求领取邮件奖励
   * @param self 玩家自己
   * @param mailId 邮件id
   * @return RewardResultSetProto
   */
  @Cmd(2)
  fun reqMailReward(self: PlayerManager.Self, mailId: Long): RequestResult<RewardResultSetProto> = RequestResult {
    service.reqMailReward(self, mailId)
  }

  /**
   * 推送新邮件数量
   */
  @Cmd(101)
  lateinit var newMail: Push<Int>
}
