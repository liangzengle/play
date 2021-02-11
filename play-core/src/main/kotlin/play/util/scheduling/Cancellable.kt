package play.util.scheduling

import java.util.concurrent.ScheduledFuture

/**
 *
 * @author LiangZengle
 */
interface Cancellable {

  fun cancel(): Boolean

  fun isCancelled(): Boolean

  companion object {
    val cancelled = object : Cancellable {
      override fun cancel() = false

      override fun isCancelled(): Boolean = true
    }

    @JvmStatic
    fun from(f: ScheduledFuture<*>): Cancellable {
      return if (f.isCancelled) {
        cancelled
      } else {
        object : Cancellable {
          override fun cancel(): Boolean = f.cancel(false)
          override fun isCancelled(): Boolean = f.isCancelled
        }
      }
    }

    @Suppress("NOTHING_TO_INLINE")
    inline fun ScheduledFuture<*>.toCancellable(): Cancellable = from(this)
  }
}
