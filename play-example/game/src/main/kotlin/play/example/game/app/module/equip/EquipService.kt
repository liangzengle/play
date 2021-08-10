package play.example.game.app.module.equip

import play.example.common.StatusCode
import play.example.game.app.module.player.Self
import play.util.control.Result2
import play.util.control.ok
import javax.inject.Named
import javax.inject.Singleton

@Singleton
@Named
class EquipService {

  fun putOn(self: Self, equipUid: Int, pos: Int) = StatusCode.Failure

  fun f(): Result2<String> {
    return ok("")
  }
}
