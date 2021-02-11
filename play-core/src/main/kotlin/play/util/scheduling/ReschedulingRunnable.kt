package play.util.scheduling

import java.lang.reflect.UndeclaredThrowableException
import java.time.Duration
import java.time.LocalDateTime
import play.util.time.currentDateTime
import play.util.time.currentMillis
import play.util.time.toMillis

/**
 * Created by LiangZengle on 2020/2/20.
 */
class ReschedulingRunnable(
  task: Runnable,
  private val trigger: Trigger,
  private val scheduler: Scheduler,
  errorHandler: ErrorHandler
) : DelegatingErrorHandlingRunnable(task, errorHandler), Cancellable {

  private val triggerContext = SimpleTriggerContext()
  private val triggerContextMonitor = Any()
  private var scheduledExecutionTime: LocalDateTime? = null
  private var currentFuture: Cancellable? = null

  fun schedule(): Cancellable {
    synchronized(triggerContextMonitor) {
      scheduledExecutionTime = trigger.nextExecutionTime(triggerContext)
      return scheduledExecutionTime?.let {
        val initialDelay = it.toMillis() - currentMillis()
        currentFuture = scheduler.schedule(Duration.ofMillis(initialDelay), this::run)
        this
      } ?: Cancellable.cancelled
    }
  }

  override fun run() {
    val actualExecutionTime = currentDateTime()
    super.run()
    if (cancelSchedule) {
      currentFuture?.cancel()
    }
    val completionTime = currentDateTime()
    synchronized(triggerContextMonitor) {
      triggerContext.update(scheduledExecutionTime, actualExecutionTime, completionTime)
      if (!currentFuture!!.isCancelled()) schedule()
    }
  }

  override fun cancel(): Boolean {
    synchronized(triggerContextMonitor) {
      val f = currentFuture
      return f != null && f.cancel()
    }
  }

  override fun isCancelled(): Boolean {
    synchronized(triggerContextMonitor) {
      val f = currentFuture
      return f != null && f.isCancelled()
    }
  }
}

sealed class DelegatingErrorHandlingRunnable(
  private val delegate: Runnable,
  private val errorHandler: ErrorHandler
) : Runnable {
  protected var cancelSchedule = false

  override fun run() {
    try {
      this.delegate.run()
    } catch (ex: Exception) {
      when (ex) {
        CancelScheduleException -> {
          cancelSchedule = true
          this.errorHandler.handleError(ex)
        }
        is UndeclaredThrowableException -> this.errorHandler.handleError(ex.undeclaredThrowable)
        else -> this.errorHandler.handleError(ex)
      }
    }
  }

  override fun toString(): String = "DelegatingErrorHandlingRunnable for " + this.delegate
}
