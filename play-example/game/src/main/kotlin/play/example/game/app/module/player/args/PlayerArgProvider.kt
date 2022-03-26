package play.example.game.app.module.player.args

import play.example.game.app.module.player.PlayerManager.Self

abstract class PlayerArgProvider {

  abstract val key: String

  abstract fun getValue(self: Self): Int
}
