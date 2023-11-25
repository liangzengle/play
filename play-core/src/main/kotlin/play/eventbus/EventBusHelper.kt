package play.eventbus

import play.util.reflect.TypeHierarchy

/**
 *
 * @author LiangZengle
 */
object EventBusHelper {

  /**
   * 从[subscribersMap]中获取[eventType]及其父类的监听器
   * @param eventType 事件类型
   * @param subscribersMap 当前所有的监听器
   * @return [eventType]及其父类的监听器
   */
  @JvmStatic
  fun <T> getSubscribers(eventType: Class<*>, subscribersMap: (Class<*>) -> Iterable<T>?): Iterator<T> {
    val eventTypes = TypeHierarchy.get(eventType, includeSelf = true, includeObject = false)
    return eventTypes.asSequence().flatMap { subscribersMap(it) ?: emptyList() }.iterator()
  }
}
