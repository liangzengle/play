package play.util.concurrent

import org.slf4j.MDC
import java.util.concurrent.Callable
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future

/**
 *
 * @author LiangZengle
 */
object MDCWrapper {

  fun wrap(executor: Executor): Executor {
    if (executor is MDCExecutor) {
      return executor
    }
    return MDCExecutor(executor)
  }

  fun wrap(executor: ExecutorService): ExecutorService {
    if (executor is MDCExecutorService) {
      return executor
    }
    return MDCExecutorService(executor)
  }

  fun wrap(runnable: Runnable): Runnable {
    return object : Runnable {
      private val mdc = MDC.getCopyOfContextMap()
      override fun run() {
        val prev = MDC.getCopyOfContextMap()
        try {
          MDC.setContextMap(mdc)
          runnable.run()
        } finally {
          MDC.setContextMap(prev)
        }
      }
    }
  }

  fun <V> wrap(callable: Callable<V>): Callable<V> {
    return object : Callable<V> {
      private val mdc = MDC.getCopyOfContextMap()
      override fun call(): V {
        val prev = MDC.getCopyOfContextMap()
        try {
          MDC.setContextMap(mdc)
          return callable.call()
        } finally {
          MDC.setContextMap(prev)
        }
      }
    }
  }

  private open class MDCExecutor(val underlying: Executor) : Executor {
    override fun execute(command: Runnable) {
      underlying.execute(wrap(command))
    }
  }

  private class MDCExecutorService(private val _underlying: ExecutorService) : MDCExecutor(_underlying),
    ExecutorService by _underlying {

    override fun execute(command: Runnable) {
      _underlying.execute(command)
    }

    override fun <T : Any?> submit(task: Runnable, result: T): Future<T> {
      return _underlying.submit(wrap(task), result)
    }

    override fun <T : Any?> submit(task: Callable<T>): Future<T> {
      return _underlying.submit(wrap(task))
    }

    override fun submit(task: Runnable): Future<*> {
      return _underlying.submit(wrap(task))
    }
  }
}
