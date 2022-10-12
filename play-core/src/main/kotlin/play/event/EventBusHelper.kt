package play.event

import com.google.common.reflect.TypeToken
import play.util.LambdaClassValue
import play.util.collection.toImmutableList
import java.util.*

/**
 *
 *
 * @author LiangZengle
 */
object EventBusHelper {

  private val flattenHierarchyCache = LambdaClassValue { type ->
    val types = TypeToken.of(type).types.rawTypes().asSequence().filter { it !== Any::class.java }.toImmutableList()
    assert(types.isNotEmpty())
    if (types.size == 1) types[0] else types
  }

  @JvmStatic
  fun <T> getSubscribers(eventType: Class<*>, subscribersMap: (Class<*>) -> Iterable<T>?): Iterator<T> {
    val eventTypes = flattenHierarchyCache.get(eventType)
    if (eventTypes === eventType) {
      return subscribersMap(eventType)?.iterator() ?: Collections.emptyIterator()
    }
    @Suppress("UNCHECKED_CAST")
    eventTypes as List<Class<*>>
    return eventTypes.asSequence().flatMap { subscribersMap(it) ?: emptyList() }.iterator()
  }
}
