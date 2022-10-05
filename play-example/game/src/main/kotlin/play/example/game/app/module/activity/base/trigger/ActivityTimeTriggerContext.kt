package play.example.game.app.module.activity.base.trigger

import org.springframework.stereotype.Component
import play.example.game.app.module.server.ServerService
import play.util.time.Time
import java.time.LocalDateTime

/**
 *
 * @author LiangZengle
 */
@Component
class ActivityTimeTriggerContext(private val serverService: ServerService) {

  fun getBaseTime(key: ActivityBaseTimeKey): LocalDateTime? {
    return when (key) {
      ActivityBaseTimeKey.NOW -> Time.currentDateTime()
      ActivityBaseTimeKey.SEVER_OPEN -> serverService.getOpenDate()?.atStartOfDay()
      ActivityBaseTimeKey.SERVER_MERGE -> serverService.getMergeDate()?.atStartOfDay()
    }
  }

  fun isForeverOpen(trigger: ActivityTimeTrigger) = trigger is ForeverTime && trigger.isOpen()

  fun isForeverClose(trigger: ActivityTimeTrigger) = trigger is ForeverTime && trigger.isClose()
}
