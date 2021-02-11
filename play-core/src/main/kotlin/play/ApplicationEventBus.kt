package play

import com.google.common.eventbus.EventBus
import java.util.concurrent.Executor
import play.util.concurrent.CommonPool

/**
 * A Global EventBus
 * @author LiangZengle
 */
@Suppress("UnstableApiUsage")
object ApplicationEventBus : EventBus("application") {

  // override to get rid of `Beta` warning
  override fun register(`object`: Any) {
    super.register(`object`)
  }

  override fun unregister(`object`: Any) {
    super.unregister(`object`)
  }

  /**
   * 阻塞调用所有的事件接收器
   * @param event 事件
   */
  fun postBlocking(event: Any) {
    super.post(event)
  }

  /**
   * 非阻塞调用所有的事件接收器
   * @param event 事件
   */
  override fun post(event: Any) {
    CommonPool.execute { super.post(event) }
  }

  /**
   * 由指定Executor来异步调用所有的事件接收器
   *
   * @param event 事件
   * @param executor Executor
   */
  fun post(event: Any, executor: Executor) {
    executor.execute { super.post(event) }
  }
}
