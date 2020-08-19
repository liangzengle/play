package play.util.scheduling

import play.util.time.currentDateTime
import play.util.time.currentMillis
import play.util.time.toMillis
import java.lang.reflect.UndeclaredThrowableException
import java.time.LocalDateTime
import java.util.concurrent.Delayed
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * Created by LiangZengle on 2020/2/20.
 */
class ReschedulingRunnable(
  task: Runnable,
  private val trigger: Trigger,
  private val executor: ScheduledExecutorService,
  errorHandler: ErrorHandler
) : DelegatingErrorHandlingRunnable(task, errorHandler), ScheduledFuture<Any> {

  private val triggerContext = SimpleTriggerContext()
  private val triggerContextMonitor = Any()
  private var scheduledExecutionTime: LocalDateTime? = null
  private var currentFuture: ScheduledFuture<*>? = null

  fun schedule(): ScheduledFuture<*>? {
    synchronized(triggerContextMonitor) {
      scheduledExecutionTime = trigger.nextExecutionTime(triggerContext)
      return scheduledExecutionTime?.let {
        val initialDelay = it.toMillis() - currentMillis()
        currentFuture = executor.schedule(this, initialDelay, TimeUnit.MILLISECONDS)
        this
      }
    }
  }

  override fun run() {
    val actualExecutionTime = currentDateTime()
    super.run()
    if (cancelSchedule) {
      currentFuture?.cancel(false)
    }
    val completionTime = currentDateTime()
    synchronized(triggerContextMonitor) {
      triggerContext.update(scheduledExecutionTime, actualExecutionTime, completionTime)
      if (!currentFuture!!.isCancelled) schedule()
    }
  }

  override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
    synchronized(triggerContextMonitor) {
      return this.currentFuture!!.cancel(mayInterruptIfRunning)
    }
  }

  override fun isCancelled(): Boolean {
    synchronized(triggerContextMonitor) {
      return this.currentFuture!!.isCancelled
    }
  }

  override fun isDone(): Boolean {
    synchronized(triggerContextMonitor) {
      return this.currentFuture!!.isDone
    }
  }

  override fun get(): Any {
    var curr: ScheduledFuture<*>?
    synchronized(triggerContextMonitor) {
      curr = this.currentFuture
    }
    return curr!!.get()
  }

  override fun get(timeout: Long, unit: TimeUnit): Any {
    var curr: ScheduledFuture<*>?
    synchronized(triggerContextMonitor) {
      curr = this.currentFuture
    }
    return curr!!.get(timeout, unit)
  }

  override fun getDelay(unit: TimeUnit): Long {
    var curr: ScheduledFuture<*>?
    synchronized(triggerContextMonitor) {
      curr = this.currentFuture
    }
    return curr!!.getDelay(unit)
  }

  override fun compareTo(other: Delayed): Int {
    return if (this === other) 0
    else {
      val diff = getDelay(TimeUnit.MILLISECONDS) - other.getDelay(TimeUnit.MILLISECONDS)
      if (diff == 0L) 0 else if (diff < 0) -1 else 1
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
