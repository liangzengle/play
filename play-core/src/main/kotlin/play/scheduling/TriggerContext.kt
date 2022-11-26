package play.scheduling

import java.time.Clock
import java.time.Instant

interface TriggerContext {
  val clock: Clock

  /**
   * Return the last <i>scheduled</i> execution time of the task,
   * or `null` if not scheduled before.
   */
  fun lastScheduledExecution(): Instant?

  /**
   * Return the last <i>actual</i> execution time of the task,
   * or `null` if not scheduled before.
   */
  fun lastActualExecution(): Instant?

  /**
   * Return the last completion time of the task,
   * or `null` if not scheduled before.
   */
  fun lastCompletion(): Instant?
}

internal class SimpleTriggerContext(
  override val clock: Clock,
  @Volatile private var lastScheduledExecutionTime: Instant?,
  @Volatile private var lastActualExecutionTime: Instant?,
  @Volatile private var lastCompletionTime: Instant?
) : TriggerContext {

  constructor(clock: Clock) : this(clock, null, null, null)

  fun update(
    lastScheduledExecutionTime: Instant?,
    lastActualExecutionTime: Instant?,
    lastCompletionTime: Instant?
  ) {
    this.lastScheduledExecutionTime = lastScheduledExecutionTime
    this.lastActualExecutionTime = lastActualExecutionTime
    this.lastCompletionTime = lastCompletionTime
  }

  override fun lastScheduledExecution(): Instant? = lastScheduledExecutionTime
  override fun lastActualExecution(): Instant? = lastActualExecutionTime
  override fun lastCompletion(): Instant? = lastCompletionTime
}
