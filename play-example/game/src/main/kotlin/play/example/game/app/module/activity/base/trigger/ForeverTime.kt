package play.example.game.app.module.activity.base.trigger

import java.time.LocalDateTime

/**
 *
 * @author LiangZengle
 */
class ForeverTime(val state: State) : ActivityTimeTrigger {
  enum class State {
    OPEN,
    CLOSE
  }

  override fun nextTriggerTime(baseTime: LocalDateTime, ctx: ActivityTimeTriggerContext): LocalDateTime? {
    return if (state == State.OPEN) LocalDateTime.MIN else LocalDateTime.MAX
  }

  fun isOpen() = state == State.OPEN

  fun isClose() = state == State.CLOSE

  override fun toString(): String {
    return "ForeverTime(state=$state)"
  }
}
