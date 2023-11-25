package play.spring


import play.util.concurrent.PlayFuture
import play.util.time.Time
import java.time.Duration
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Bean异步初始化支持, 仅当所有异步初始化操作全部成功执行完成后Spring才算初始化完成
 *
 * ```
 *  class MyService(private val asyncInitializing: AsyncInitializingSupport) : SmartInitializingSingleton {
 *      @PostConstruct
 *      fun init() {
 *        val future = doSomethingAsync()
 *        asyncInitializing.register("do something", future)
 *      }
 *      // or
 *      override fun afterSingletonsInstantiated() {
 *        val future = doSomethingAsync()
 *        asyncInitializing.register("do something", future)
 *      }
 *  }
 * ```
 * @author LiangZengle
 */
class AsyncInitializingSupport {

  companion object : WithLogger() {
    const val Timeout = "spring.asyncInitializing.timeout"
  }

  private var futures: MutableList<Pair<String, PlayFuture<*>>>? = arrayListOf()

  fun register(name: String, future: PlayFuture<*>) {
    val futureList = futures ?: throw IllegalStateException("Too late to register")
    futureList.add(name to future)
  }

  internal fun await(timeout: Duration) {
    val futureList = futures ?: throw IllegalStateException("`futures` should not be null at this point.")
    futures = null

    val list = LinkedList(futureList)
    val timeoutTime = Time.currentMillis() + timeout.toMillis()
    while (list.isNotEmpty()) {
      val iterator = list.iterator()
      while (iterator.hasNext()) {
        val (name, future) = iterator.next()
        if (!future.isCompleted()) {
          continue
        }
        if (future.isSuccess()) {
          logger.debug("Initializing [{}] done", name)
          iterator.remove()
        } else if (future.isFailed()) {
          logger.error("Initializing [{}] failed", name)
          future.getNowOrThrow()
        }
      }
      if (list.isEmpty()) {
        break
      }
      if (Time.currentMillis() < timeoutTime) {
        val incompleted = list.asSequence().filter { !it.second.isCompleted() }.map { it.first }.toList()
        throw TimeoutException("Async initializing bean did not complete in ${timeout.toMillis()}ms: $incompleted")
      }
      TimeUnit.SECONDS.sleep(1)
    }
  }
}
