/*
 * Copyright 2002-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package play.scheduling

import play.util.time.Time.toMillis
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.ScheduledExecutorService
import javax.annotation.Nullable
import javax.annotation.concurrent.GuardedBy

/**
 * Internal adapter that reschedules an underlying [Runnable] according
 * to the next execution time suggested by a given [Trigger].
 *
 *
 * Necessary because a native [ScheduledExecutorService] supports
 * delay-driven execution only. The flexibility of the [Trigger] interface
 * will be translated onto a delay for the next execution time (repeatedly).
 *
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @since 3.0
 */
internal class ReschedulingRunnable(
  delegate: Runnable,
  private val trigger: Trigger,
  clock: Clock,
  private val scheduler: Scheduler,
  errorHandler: ErrorHandler
) : DelegatingErrorHandlingRunnable(delegate, errorHandler), Cancellable, Runnable {
  private val triggerContext: SimpleTriggerContext = SimpleTriggerContext(clock)

  @Nullable
  private var currentFuture: Cancellable? = null

  @Nullable
  private var scheduledExecutionTime: LocalDateTime? = null
  private val triggerContextMonitor = Any()

  @Nullable
  fun schedule(): Cancellable {
    synchronized(triggerContextMonitor) {
      scheduledExecutionTime = trigger.nextExecutionTime(triggerContext)
      if (scheduledExecutionTime == null) {
        return Cancellable.cancelled
      }
      val initialDelay = scheduledExecutionTime!!.toMillis() - triggerContext.clock.millis()
      currentFuture = scheduler.schedule(Duration.ofMillis(initialDelay), this)
      return this
    }
  }

  @GuardedBy("triggerContextMonitor")
  private fun obtainCurrentFuture(): Cancellable {
    check(currentFuture != null) { "No scheduled future" }
    return currentFuture!!
  }

  override fun run() {
    val actualExecutionTime = LocalDateTime.now(triggerContext.clock)
    super.run()
    val completionTime = LocalDateTime.now(triggerContext.clock)
    synchronized(triggerContextMonitor) {
      val executionTime = scheduledExecutionTime
      check(executionTime != null) { "No scheduled execution" }
      triggerContext.update(executionTime, actualExecutionTime, completionTime)
      if (!obtainCurrentFuture().isCancelled()) {
        schedule()
      }
    }
  }

  override fun isCancelled(): Boolean {
    synchronized(triggerContextMonitor) { return obtainCurrentFuture().isCancelled() }
  }

  override fun cancel(): Boolean {
    synchronized(triggerContextMonitor) {
      return obtainCurrentFuture().cancel()
    }
  }

  override fun canceller(): Canceller<*> = CancellableCanceller

  override fun taskHandle(): Any = this
}
