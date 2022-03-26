package play.example.game.app.module.player.args

import play.example.game.app.module.player.PlayerManager.Self

class PlayerArgs(private val self: Self, private val argProviders: Map<String, PlayerArgProvider>) : Map<String, Int> {
  private val cache = HashMap<String, Int>(4)

  override val entries: Set<Map.Entry<String, Int>>
    get() = throw UnsupportedOperationException()
  override val keys: Set<String>
    get() = throw UnsupportedOperationException()
  override val size: Int
    get() = throw UnsupportedOperationException()
  override val values: Collection<Int>
    get() = throw UnsupportedOperationException()

  override fun containsKey(key: String): Boolean {
    return argProviders.containsKey(key)
  }

  override fun containsValue(value: Int): Boolean {
    throw UnsupportedOperationException()
  }

  override fun get(key: String): Int {
    var value = cache[key]
    if (value != null) {
      return value
    }
    value = argProviders[key]?.getValue(self) ?: 0
    cache[key] = value
    return value
  }

  override fun isEmpty(): Boolean {
    throw UnsupportedOperationException()
  }
}
