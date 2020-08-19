package play.example.module.player.event

import play.example.module.player.Self

interface PlayerEventListener {

  fun playerEventReceive(): PlayerEventReceive
}

data class PlayerEventReceive(val receive: Map<Class<PlayerEvent>, (Self, PlayerEvent) -> Unit>)

class PlayerEventReceiveBuilder {
  private val map: MutableMap<Class<PlayerEvent>, (Self, PlayerEvent) -> Unit> = hashMapOf()

  inline fun <reified T : PlayerEvent> match(noinline f: (Self, T) -> Unit): PlayerEventReceiveBuilder {
    match(T::class.java, f)
    return this
  }

  inline fun <reified T : PlayerEvent> match(noinline f: (Self) -> Unit): PlayerEventReceiveBuilder {
    match(T::class.java) { self, _: T -> f(self) }
    return this
  }

  fun <T : PlayerEvent> match(eventType: Class<T>, f: (Self, T) -> Unit): PlayerEventReceiveBuilder {
    add(eventType, f)
    return this
  }

  fun <T : PlayerEvent> match(eventType: Class<T>, f: (Self) -> Unit): PlayerEventReceiveBuilder {
    add(eventType) { self, _: T -> f(self) }
    return this
  }

  @Suppress("UNCHECKED_CAST")
  private fun <T> add(clazz: Class<T>, f: (Self, T) -> Unit) {
    map[clazz as Class<PlayerEvent>] = f as ((Self, PlayerEvent) -> Unit)
  }

  fun build(): PlayerEventReceive {
    return PlayerEventReceive(map.toMap())
  }
}
