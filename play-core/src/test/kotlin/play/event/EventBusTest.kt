package play.event

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger

/**
 *
 *
 * @author LiangZengle
 */
internal class EventBusTest {

  @Test
  fun post() {
    val eventBus = EventBus()
    eventBus.register(EventListenerA())
    eventBus.register(EventListenerB())

    val eventA = EventA()
    eventBus.post(eventA)
    Assertions.assertEquals(eventA.counter.get(), 1)

    val eventB = EventB()
    eventBus.post(eventB)
    Assertions.assertEquals(eventB.counter.get(), 2)

    val eventC = EventC()
    eventBus.post(eventC)
    Assertions.assertEquals(eventC.counter.get(), 1)

    val eventD = EventD()
    eventBus.post(eventD)
    Assertions.assertEquals(eventD.counter.get(), 0)
  }

  open class EventA {
    val counter = AtomicInteger()
  }

  class EventB : EventA()

  class EventC : EventA()

  class EventD {
    val counter = AtomicInteger()
  }

  class EventListenerA : EventListener {
    override fun eventReceive(): EventReceive {
      return newEventReceiveBuilder()
        .match(::onEvent)
        .build()
    }

    private fun onEvent(event: EventA) {
      event.counter.incrementAndGet()
    }
  }

  class EventListenerB : EventListener {
    override fun eventReceive(): EventReceive {
      return newEventReceiveBuilder()
        .match(::onEvent)
        .build()
    }

    private fun onEvent(event: EventB) {
      event.counter.incrementAndGet()
    }
  }
}
