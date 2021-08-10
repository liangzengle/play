package play.scheduling

import java.util.concurrent.Delayed
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * @author LiangZengle
 */
interface Cancellable {

  fun cancel(): Boolean

  fun isCancelled(): Boolean

  companion object {
    @JvmStatic
    val cancelled = object : Cancellable {
      override fun cancel() = false

      override fun isCancelled(): Boolean = true
    }

    @JvmStatic
    fun from(f: ScheduledFuture<*>): Cancellable {
      return if (f.isCancelled) {
        cancelled
      } else {
        ScheduledFutureCancellableAdapter(f)
      }
    }

    @JvmStatic
    fun toScheduledFuture(cancellable: Cancellable): ScheduledFuture<*> {
      return CancellableScheduledFutureAdapter(cancellable)
    }

    @Suppress("NOTHING_TO_INLINE")
    inline fun ScheduledFuture<*>.toCancellable(): Cancellable = from(this)
  }
}

@Suppress("NOTHING_TO_INLINE")
inline fun Cancellable.toScheduledFuture(): ScheduledFuture<*> {
  return Cancellable.toScheduledFuture(this)
}

private class CancellableScheduledFutureAdapter(private val cancellable: Cancellable) : ScheduledFuture<Void> {
  override fun compareTo(other: Delayed?): Int {
    return 0
  }

  override fun getDelay(unit: TimeUnit): Long {
    return 0
  }

  override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
    return cancellable.cancel()
  }

  override fun isCancelled(): Boolean {
    return cancellable.isCancelled()
  }

  override fun isDone(): Boolean {
    throw UnsupportedOperationException()
  }

  override fun get(): Void {
    throw UnsupportedOperationException()
  }

  override fun get(timeout: Long, unit: TimeUnit): Void {
    throw UnsupportedOperationException()
  }

}

private class ScheduledFutureCancellableAdapter(private val f: ScheduledFuture<*>) : Cancellable {
  override fun cancel(): Boolean {
    return f.cancel(false)
  }

  override fun isCancelled(): Boolean {
    return f.isCancelled
  }
}
