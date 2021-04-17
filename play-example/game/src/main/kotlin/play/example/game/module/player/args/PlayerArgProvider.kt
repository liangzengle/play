package play.example.game.module.player.args

import play.example.game.module.player.Self

abstract class PlayerArgProvider {

  abstract val key: String

  abstract fun getValue(self: Self): Int
}
