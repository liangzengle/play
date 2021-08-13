package play.example.game.app.module.mail

import org.springframework.stereotype.Component
import play.example.game.app.module.ModuleId
import play.mvc.AbstractController
import play.mvc.Cmd
import play.mvc.Controller
import play.mvc.Push


@Controller(ModuleId.Mail)
@Component
class MailController : AbstractController(ModuleId.Mail) {

  @Cmd(101)
  lateinit var newMail: Push<Int>

  @Cmd(102)
  lateinit var forceDeleteTrashMails: Push<Int>
}
