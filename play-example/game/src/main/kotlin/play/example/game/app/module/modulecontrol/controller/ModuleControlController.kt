package play.example.game.app.module.modulecontrol.controller

import play.example.game.app.module.ModuleId
import play.mvc.AbstractController
import play.mvc.Cmd
import play.mvc.Controller
import play.mvc.Push
import javax.inject.Named
import javax.inject.Singleton

/**
 *
 * @author LiangZengle
 */
@Singleton
@Named
@Controller(ModuleId.ModuleControl)
class ModuleControlController : AbstractController(ModuleId.ModuleControl) {

  /**
   * 推送模块开启
   */
  @Cmd(1)
  lateinit var moduleOpen: Push<Int>
}
