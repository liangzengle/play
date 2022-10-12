package play.event

/**
 *
 *
 * @author LiangZengle
 */
interface EventBus {

  fun publish(event: Any)

  fun <T> subscribe(eventType: Class<T>, action: (T) -> Unit)

  fun <T> subscribe0(eventType: Class<T>, action: () -> Unit) {
    subscribe(eventType) { action() }
  }
}
