package play.example.game.app.module.equip

import org.springframework.stereotype.Component
import play.example.game.app.module.ModuleId
import play.example.game.app.module.player.PlayerManager.Self
import play.mvc.AbstractController
import play.mvc.Cmd
import play.mvc.Controller
import play.mvc.RequestResult

@Component
@Controller(ModuleId.Equip)
class EquipController(private val service: EquipService) : AbstractController(ModuleId.Equip) {

  @Cmd(1)
  fun putOn(self: Self, equipUid: Int, pos: Int) = RequestResult {
    service.putOn(self, equipUid, pos)
  }

//  @Cmd(101)
//  lateinit var info: Push<Int32Value>
}
