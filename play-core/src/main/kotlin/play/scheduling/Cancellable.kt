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

  fun canceller(): Canceller<*>

  fun taskHandle(): Any

  companion object {
    @JvmStatic
    val cancelled = object : Cancellable {
      override fun cancel() = false

      override fun isCancelled(): Boolean = true

      override fun canceller(): CancellableCanceller = CancellableCanceller

      override fun taskHandle(): Any = this
    }

    @JvmStatic
    val completed = object : Cancellable {
      override fun cancel() = false

      override fun isCancelled(): Boolean = true

      override fun canceller(): CancellableCanceller = CancellableCanceller

      override fun taskHandle(): Any = this
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

private class ScheduledFutureCancellableAdapter(private val f: ScheduledFuture<*>) : Cancellable, ScheduledFutureLike {
  override fun cancel(): Boolean {
    return f.cancel(false)
  }

  override fun isDone(): Boolean {
    return f.isDone
  }

  override fun isCancelled(): Boolean {
    return f.isCancelled || f.isDone
  }

  override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
    return cancel()
  }

  override fun canceller(): ScheduledFutureCanceller = ScheduledFutureCanceller

  override fun taskHandle(): ScheduledFuture<*> = f
}
