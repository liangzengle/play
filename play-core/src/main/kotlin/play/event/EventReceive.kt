package play.event

import com.google.common.collect.ImmutableMap

/**
 *
 *
 * @author LiangZengle
 */
class EventReceive(val receive: Map<Class<*>, (Any) -> Unit>)

class EventReceiveBuilder {
  private val builder = ImmutableMap.builder<Class<*>, (Any) -> Unit>()

  inline fun <reified T : Any> match(noinline f: (T) -> Unit): EventReceiveBuilder {
    match(T::class.java, f)
    return this
  }

  inline fun <reified T : Any> match(noinline f: () -> Unit): EventReceiveBuilder {
    match(T::class.java) { _: T -> f() }
    return this
  }

  fun <T : Any> match(eventType: Class<T>, f: (T) -> Unit): EventReceiveBuilder {
    add(eventType, f)
    return this
  }

  fun <T : Any> match(eventType: Class<T>, f: () -> Unit): EventReceiveBuilder {
    add(eventType) { _: T -> f() }
    return this
  }

  @Suppress("UNCHECKED_CAST")
  private fun <T> add(clazz: Class<T>, f: (T) -> Unit) {
    val eventType = clazz as Class<*>
    val listener = f as ((Any) -> Unit)
    builder.put(eventType, listener)
  }

  fun build(): EventReceive {
    return EventReceive(builder.build())
  }
}
