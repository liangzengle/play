package play.example.game.app.module.activity.base.trigger

import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.LocalDateTime

/**
 *
 * @author LiangZengle
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
sealed interface ActivityTimeTrigger {

  fun nextTriggerTime(baseTime: LocalDateTime, ctx: ActivityTimeTriggerContext): LocalDateTime?
}
