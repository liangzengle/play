package play.example.module.player.args

import play.example.module.player.Self


abstract class PlayerArgProvider {

  abstract val key: String

  abstract fun getValue(self: Self): Int
}
