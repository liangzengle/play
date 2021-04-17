package play.example.game.module.equip

import javax.inject.Inject
import javax.inject.Singleton
import play.example.game.module.ModuleId
import play.example.game.module.player.Self
import play.mvc.AbstractController
import play.mvc.Cmd
import play.mvc.Controller
import play.mvc.RequestResult

@Singleton
@Controller(ModuleId.Equip)
class EquipController @Inject constructor(private val service: EquipService) : AbstractController(ModuleId.Equip) {

  @Cmd(1)
  fun putOn(self: Self, equipUid: Int, pos: Int) = RequestResult {
    service.putOn(self, equipUid, pos)
  }

//  @Cmd(101)
//  lateinit var info: Push<Int32Value>
}
