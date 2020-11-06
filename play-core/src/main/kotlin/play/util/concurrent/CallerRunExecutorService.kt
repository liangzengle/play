package play.util.concurrent

import java.util.concurrent.*
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater

/**
 *
 * @author LiangZengle
 */
class CallerRunExecutorService : ExecutorService {

  @Volatile
  private var shutdown = 0

  companion object {
    @JvmStatic
    private val SHUTDOWN_UPDATER =
      AtomicIntegerFieldUpdater.newUpdater(CallerRunExecutorService::class.java, "shutdown")
  }

  override fun execute(command: Runnable) {
    command.run()
  }

  override fun shutdown() {
    SHUTDOWN_UPDATER.compareAndSet(this, 0, 1)
  }

  override fun shutdownNow(): List<Runnable> {
    shutdown()
    return emptyList()
  }

  override fun isShutdown(): Boolean {
    return shutdown != 0
  }

  override fun isTerminated(): Boolean {
    return shutdown != 0
  }

  override fun awaitTermination(timeout: Long, unit: TimeUnit): Boolean {
    require(isShutdown) { "should call `shutdown` first." }
    return true
  }

  override fun <T : Any?> submit(task: Callable<T>): CompletableFuture<T> {
    return try {
      CompletableFuture.completedFuture(task.call())
    } catch (e: Exception) {
      CompletableFuture.failedFuture(e)
    }
  }

  override fun <T : Any?> submit(task: Runnable, result: T): CompletableFuture<T> {
    return try {
      task.run()
      CompletableFuture.completedFuture(result)
    } catch (e: Exception) {
      CompletableFuture.failedFuture(e)
    }
  }

  override fun submit(task: Runnable): Future<Any?> {
    return try {
      task.run()
      CompletableFuture.completedFuture(null)
    } catch (e: Exception) {
      CompletableFuture.failedFuture(e)
    }
  }

  @Suppress("UNCHECKED_CAST")
  override fun <T : Any?> invokeAll(tasks: MutableCollection<out Callable<T>>): MutableList<Future<T>> {
    return tasks.map(::submit) as MutableList<Future<T>>
  }

  override fun <T : Any?> invokeAll(
    tasks: MutableCollection<out Callable<T>>,
    timeout: Long,
    unit: TimeUnit
  ): MutableList<Future<T>> {
    return invokeAll(tasks)
  }

  override fun <T : Any?> invokeAny(tasks: MutableCollection<out Callable<T>>): T? {
    if (tasks.isEmpty()) {
      throw IllegalArgumentException("tasks is empty")
    }
    for (task in tasks) {
      val f = submit(task)
      if (!f.isCompletedExceptionally) {
        return f.get()
      }
    }
    return null
  }

  override fun <T : Any?> invokeAny(tasks: MutableCollection<out Callable<T>>, timeout: Long, unit: TimeUnit): T? {
    return invokeAny(tasks)
  }
}
