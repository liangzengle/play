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

  @Cmd(1)
  fun reqMailList(self: PlayerManager.Self, num: Int): RequestResult<MailListProto> = RequestResult.ok {
    service.reqMailList(self, num)
  }

  @Cmd(2)
  fun reqMailReward(self: PlayerManager.Self, mailId: Long): RequestResult<RewardResultSetProto> = RequestResult {
    service.reqMailReward(self, mailId)
  }

  @Cmd(101)
  lateinit var newMail: Push<Int>

  @Cmd(102)
  lateinit var forceDeleteTrashMails: Push<Int>
}
