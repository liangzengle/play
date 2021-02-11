package play.util.scheduling

import java.time.LocalDateTime

interface TriggerContext {
  /**
   * Return the last <i>scheduled</i> execution time of the task,
   * or `null` if not scheduled before.
   */
  fun lastScheduledExecutionTime(): LocalDateTime?

  /**
   * Return the last <i>actual</i> execution time of the task,
   * or `null` if not scheduled before.
   */
  fun lastActualExecutionTime(): LocalDateTime?

  /**
   * Return the last completion time of the task,
   * or `null` if not scheduled before.
   */
  fun lastCompletionTime(): LocalDateTime?
}

internal class SimpleTriggerContext(
  @Volatile private var lastScheduledExecutionTime: LocalDateTime?,
  @Volatile private var lastActualExecutionTime: LocalDateTime?,
  @Volatile private var lastCompletionTime: LocalDateTime?
) : TriggerContext {

  constructor() : this(null, null, null)

  fun update(
    lastScheduledExecutionTime: LocalDateTime?,
    lastActualExecutionTime: LocalDateTime?,
    lastCompletionTime: LocalDateTime?
  ) {
    this.lastScheduledExecutionTime = lastScheduledExecutionTime
    this.lastActualExecutionTime = lastActualExecutionTime
    this.lastCompletionTime = lastCompletionTime
  }

  override fun lastScheduledExecutionTime(): LocalDateTime? = lastScheduledExecutionTime
  override fun lastActualExecutionTime(): LocalDateTime? = lastActualExecutionTime
  override fun lastCompletionTime(): LocalDateTime? = lastCompletionTime
}
