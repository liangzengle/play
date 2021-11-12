package play.scheduling

interface ScheduledFutureLike {

  fun isDone(): Boolean

  fun isCancelled(): Boolean

  fun cancel(mayInterruptIfRunning: Boolean): Boolean
}
