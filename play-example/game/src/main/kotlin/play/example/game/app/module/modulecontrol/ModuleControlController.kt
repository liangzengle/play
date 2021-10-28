package play.example.game.app.module.modulecontrol

import org.springframework.stereotype.Component
import play.example.game.app.module.ModuleId
import play.mvc.AbstractController
import play.mvc.Cmd
import play.mvc.Controller
import play.mvc.Push

/**
 *
 * @author LiangZengle
 */
@Component
@Controller(ModuleId.ModuleControl)
class ModuleControlController : AbstractController(ModuleId.ModuleControl) {

  /**
   * 推送模块开启
   */
  @Cmd(1)
  lateinit var moduleOpen: Push<Int>
}
