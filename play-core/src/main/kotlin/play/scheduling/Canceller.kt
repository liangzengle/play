package play.scheduling

import java.util.concurrent.ScheduledFuture

interface Canceller<T : Any> {
  fun cancel(target: T): Boolean
}

object ScheduledFutureCanceller : Canceller<ScheduledFuture<*>> {
  override fun cancel(target: ScheduledFuture<*>): Boolean {
    return target.cancel(false)
  }
}

object CancellableCanceller : Canceller<Cancellable> {
  override fun cancel(target: Cancellable): Boolean {
    return target.cancel()
  }
}
