package play.example.game.app.module.player.event

import com.google.common.collect.ImmutableMap
import play.example.game.app.module.player.Self

interface PlayerEventListener {

  fun playerEventReceive(): PlayerEventReceive

  fun newPlayerEventReceiveBuilder(): PlayerEventReceiveBuilder = PlayerEventReceiveBuilder()
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
    val eventType = clazz as Class<PlayerEvent>
    val listener = f as ((Self, PlayerEvent) -> Unit)
    val prev = map.put(eventType, listener)
    if (prev != null) {
      throw IllegalStateException("重复监听事件: ${eventType.simpleName}")
    }
  }

  fun build(): PlayerEventReceive {
    return PlayerEventReceive(ImmutableMap.copyOf(map))
  }
}
