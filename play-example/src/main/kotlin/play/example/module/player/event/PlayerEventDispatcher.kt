package play.example.module.player.event

import com.google.common.collect.ImmutableListMultimap
import play.example.module.player.Self
import play.getLogger
import play.inject.Injector
import play.util.reflect.isFinal
import play.util.unsafeCast
import javax.inject.Inject
import javax.inject.Provider

class PlayerEventDispatcher(injector: Injector) {
  private val logger = getLogger()

  private val oneForOne: Map<Class<PlayerEvent>, List<(Self, PlayerEvent) -> Unit>>
  private val oneForMany: List<Pair<Class<PlayerEvent>, List<(Self, PlayerEvent) -> Unit>>>

  init {
    val oneForOneBuilder = ImmutableListMultimap.builder<Class<PlayerEvent>, (Self, PlayerEvent) -> Unit>()
    val oneForManyBuilder = ImmutableListMultimap.builder<Class<PlayerEvent>, (Self, PlayerEvent) -> Unit>()
    for (listener in injector.instancesOf(PlayerEventListener::class)) {
      for ((eventType, receiver) in listener.playerEventReceive().receive) {
        if (eventType.isFinal()) {
          oneForOneBuilder.put(eventType, receiver)
        } else {
          oneForOneBuilder.put(eventType, receiver)
        }
      }
    }
    oneForOne = oneForOneBuilder.build().asMap().unsafeCast()
    oneForMany = oneForManyBuilder.build().asMap().entries.map { it.key to it.value.unsafeCast() }
  }

  fun dispatch(self: Self, event: PlayerEvent) {
    val listeners = oneForOne[event.javaClass]
    if (listeners != null) {
      for (i in listeners.indices) {
        val f = listeners[i]
        catchInvoke(f, self, event)
      }
    }
    for (i in oneForMany.indices) {
      val (clazz, list) = oneForMany[i]
      if (clazz.isAssignableFrom(event.javaClass)) {
        for (j in list.indices) {
          val f = list[i]
          catchInvoke(f, self, event)
        }
      }
    }
  }

  private fun catchInvoke(f: (Self, PlayerEvent) -> Unit, self: Self, event: PlayerEvent) {
    try {
      f(self, event)
    } catch (e: Exception) {
      logger.error(e) { "事件处理失败: $event" }
    }
  }
}

class PlayerEventDispatcherProvider @Inject constructor(private val injector: Injector) :
  Provider<PlayerEventDispatcher> {

  private val bean by lazy { PlayerEventDispatcher(injector) }

  override fun get(): PlayerEventDispatcher = bean
}
