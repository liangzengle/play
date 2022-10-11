package play.example.game.app.module.mail.http

import org.springframework.stereotype.Component
import play.example.game.app.admin.AdminHttpActionManager
import play.example.game.app.module.common.model.I18nText
import play.example.game.app.module.mail.MailService
import play.example.game.app.module.player.condition.PlayerCondition
import play.example.game.app.module.reward.RewardHelper
import play.net.http.AbstractHttpController
import play.net.http.HttpResult
import play.net.http.Route
import play.util.getOrNull
import play.util.time.Time
import java.util.*

/**
 *
 *
 * @author LiangZengle
 */
@Component
class MailHttpController(
  manager: AdminHttpActionManager,
  private val mailService: MailService
) : AbstractHttpController(manager) {

  @Route("/mail")
  fun mail(
    title: String,
    content: String,
    rewards: Optional<String>,
    startTime: OptionalLong,
    expireTime: Long
  ): HttpResult {
    // TODO
    val receiveConditions = emptyList<PlayerCondition>()
    val rewardList = rewards.map(RewardHelper::parseRewardStringAsRawRewards).getOrNull() ?: emptyList()
    val st = startTime.orElse(Time.currentMillis())
    // TODO
    val logSource = 0
    mailService.createPublicMail(
      I18nText(title),
      I18nText(content),
      receiveConditions,
      rewardList,
      logSource,
      st,
      expireTime
    )
    return ok()
  }
}
