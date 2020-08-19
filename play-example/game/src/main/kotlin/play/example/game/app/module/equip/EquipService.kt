package play.example.game.app.module.equip

import org.springframework.stereotype.Component
import play.example.common.StatusCode
import play.example.game.app.module.player.PlayerManager.Self
import play.util.control.Result2
import play.util.control.ok

@Component
class EquipService {

  fun putOn(self: Self, equipUid: Int, pos: Int) = StatusCode.Failure

  fun f(): Result2<String> {
    return ok("")
  }
}
