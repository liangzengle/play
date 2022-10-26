package play.hotswap

import mu.KLogging
import org.springframework.beans.factory.config.AutowireCapableBeanFactory
import org.springframework.context.ApplicationContext
import org.springframework.context.SmartLifecycle
import play.util.unsafeCast
import java.util.concurrent.Flow

interface HotSwapResultSubscriber {
  fun onResult(result: HotSwapResult)
}

object NOOPHotSwapResultSubscriber : HotSwapResultSubscriber {
  override fun onResult(result: HotSwapResult) {
    // noop
  }
}

/**
 *
 *
 * @author LiangZengle
 */
open class DefaultHotSwapResultSubscriber(
  private val applicationContext: ApplicationContext,
  private val hotSwapWatcher: HotSwapWatcher
) : HotSwapResultSubscriber, SmartLifecycle {
  companion object : KLogging() {
    private const val STATE_STARTED = 1
    private const val STATE_COMPLETED = 2
    private const val STATE_CANCELED = 3
  }

  private val subscriber = Subscriber(::onResult)

  override fun start() {
    subscriber.state = STATE_STARTED
    hotSwapWatcher.subscribe(subscriber)
  }

  override fun stop() {
    subscriber.cancel()
  }

  override fun isRunning(): Boolean {
    return subscriber.state == STATE_STARTED
  }

  private class Subscriber(private val action: (HotSwapResult) -> Unit) : Flow.Subscriber<HotSwapResult> {
    private var s: Flow.Subscription? = null

    @Volatile
    var state = 0

    override fun onSubscribe(subscription: Flow.Subscription?) {
      s = subscription
      s?.request(Long.MAX_VALUE)
    }

    override fun onError(throwable: Throwable) {
      logger.error(throwable.message, throwable)
    }

    override fun onComplete() {
      state = STATE_COMPLETED
    }

    override fun onNext(item: HotSwapResult) {
      action(item)
    }

    fun cancel() {
      state = STATE_CANCELED
      s?.cancel()
    }
  }

  @Suppress("UNCHECKED_CAST")
  override fun onResult(result: HotSwapResult) {
    for (definedClass in result.definedClasses) {
      if (HotSwapScript::class.java.isAssignableFrom(definedClass)) {
        execute(definedClass as Class<out HotSwapScript>)
      }
    }
  }

  private fun execute(scriptClass: Class<out HotSwapScript>) {
    logger.info { "Start executing HotSwapScript: ${scriptClass.name}" }
    try {
      val script = initiate(scriptClass)
      script.execute()
      logger.info { "Finish executing HotSwapScript: ${scriptClass.name}" }
    } catch (e: Throwable) {
      logger.error(e) { "Failed to execute HotSwapScript: ${scriptClass.name}" }
    }
  }

  private fun initiate(scriptClass: Class<out HotSwapScript>): HotSwapScript {
    return applicationContext.autowireCapableBeanFactory.run {
      val bean = autowire(
        scriptClass, AutowireCapableBeanFactory.AUTOWIRE_CONSTRUCTOR, false
      )
      autowireBeanProperties(bean, AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, false)
      bean
    }.unsafeCast()
  }
}
