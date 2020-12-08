package play.example.module.equip

import javax.inject.Singleton
import play.example.module.StatusCode
import play.example.module.player.Self
import play.util.control.Result2
import play.util.control.ok

@Singleton
class EquipService {

  fun putOn(self: Self, equipUid: Int, pos: Int) = StatusCode.Failure

  fun f(): Result2<String> {
    return ok("")
  }
}
