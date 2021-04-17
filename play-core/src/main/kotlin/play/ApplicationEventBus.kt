package play

import com.google.common.eventbus.EventBus
import java.util.concurrent.Executor
import play.util.concurrent.CommonPool
import play.util.logging.getLogger

/**
 * A Global EventBus
 * @author LiangZengle
 */
@Suppress("UnstableApiUsage")
class ApplicationEventBus : EventBus("application") {

  companion object {
    @JvmStatic
    val logger = getLogger()
  }

  // override to get rid of `Beta` warning
  override fun register(listener: Any) {
    super.register(listener)
  }

  override fun unregister(listener: Any) {
    super.unregister(listener)
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
    post(event, CommonPool)
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
