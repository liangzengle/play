package play.event

/**
 * 事件总线
 *
 * @author LiangZengle
 */
interface EventBus {

  /**
   * 发布事件
   * @param event 事件
   */
  fun publish(event: Any)

  /**
   * 监听事件
   * @param eventType 事件类型
   * @param subscriber 监听器
   */
  fun <T> subscribe(eventType: Class<T>, subscriber: (T) -> Unit)

  /**
   * 监听事件
   * @param eventType 事件类型
   * @param subscriber 监听器
   */
  fun <T> subscribe0(eventType: Class<T>, subscriber: () -> Unit) {
    subscribe(eventType) { subscriber() }
  }
}

inline fun <reified T> EventBus.subscribe(noinline subscriber: (T) -> Unit) {
  this.subscribe(T::class.java, subscriber)
}

inline fun <reified T> EventBus.subscribe0(noinline subscriber: () -> Unit) {
  this.subscribe0(T::class.java, subscriber)
}
