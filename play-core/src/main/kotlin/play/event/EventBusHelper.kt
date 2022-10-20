package play.event

import play.util.reflect.TypeHierarchy

/**
 *
 *
 * @author LiangZengle
 */
object EventBusHelper {

  @JvmStatic
  fun <T> getSubscribers(eventType: Class<*>, subscribersMap: (Class<*>) -> Iterable<T>?): Iterator<T> {
    val eventTypes = TypeHierarchy.get(eventType, includeSelf = true, includeObject = false)
    return eventTypes.asSequence().flatMap { subscribersMap(it) ?: emptyList() }.iterator()
  }
}
