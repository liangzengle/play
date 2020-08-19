package play.example.module.equip

import play.example.module.player.Self
import play.util.control.Result2
import play.util.control.ok
import javax.inject.Singleton

@Singleton
class EquipService {

  fun putOn(self: Self, equipUid: Int, pos: Int) = 0

  fun f(): Result2<String> {
    return ok("")
  }
}
