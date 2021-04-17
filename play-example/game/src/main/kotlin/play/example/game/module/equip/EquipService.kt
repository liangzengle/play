package play.example.game.module.equip

import javax.inject.Singleton
import play.example.common.StatusCode
import play.example.game.module.player.Self
import play.util.control.Result2
import play.util.control.ok

@Singleton
class EquipService {

  fun putOn(self: Self, equipUid: Int, pos: Int) = StatusCode.Failure

  fun f(): Result2<String> {
    return ok("")
  }
}
