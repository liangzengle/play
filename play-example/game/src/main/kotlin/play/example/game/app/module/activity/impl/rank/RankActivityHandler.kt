package play.example.game.app.module.activity.impl.rank

import org.springframework.stereotype.Component
import play.example.game.app.module.activity.base.ActivityCache
import play.example.game.app.module.activity.base.ActivityHandler
import play.example.game.app.module.activity.base.ActivityType
import play.example.game.app.module.activity.base.entity.ActivityDataKey
import play.example.game.app.module.activity.base.entity.ActivityEntity
import play.example.game.app.module.activity.base.res.ActivityResource
import play.example.game.app.module.activity.base.stage.ActivityStage
import play.example.game.app.module.activity.impl.rank.domain.RankActivityLogSource
import play.example.game.app.module.activity.impl.rank.res.RankActivityResourceSet
import play.example.game.app.module.activity.impl.rank.res.RankActivityRewardResourceSet
import play.example.game.app.module.activity.impl.rank.res.RankActivitySettingConf
import play.example.game.app.module.common.model.I18nText
import play.example.game.app.module.mail.MailService
import play.example.game.app.module.mail.entity.Mail
import play.example.game.app.module.player.PlayerManager
import play.example.game.app.module.player.event.PlayerEvent
import play.example.game.app.module.player.event.PlayerEventBus
import play.util.unsafeCast

/**
 *
 * @author LiangZengle
 */
@Component
class RankActivityHandler(
  playerEventBus: PlayerEventBus,
  private val activityCache: ActivityCache,
  private val mailService: MailService
) : ActivityHandler {

  init {
    RankActivityResourceSet.list().asSequence().map { it.rankType.eventType() }.toSet().toSet()
      .forEach { playerEventBus.subscribe(it, ::onEvent) }
  }

  private fun onEvent(self: PlayerManager.Self, event: PlayerEvent) {
    for (activity in activityCache.getActivities(ActivityType.RANK, ActivityStage.just(ActivityStage.Start))) {
      val cfg = RankActivityResourceSet.getOrNull(activity.id) ?: continue
      if (cfg.rankType.eventType() != event.javaClass) {
        continue
      }
      val rankElem = cfg.rankType.toRankElement(event.unsafeCast())
      ActivityDataKey.getOrCreateRank(activity).update(rankElem)
    }
  }

  override fun type(): ActivityType = ActivityType.RANK

  override fun onClose(entity: ActivityEntity, resource: ActivityResource) {
    super.onClose(entity, resource)
    // 发奖
    val rankingList = entity.data.attr(ActivityDataKey.Rank).getValue() ?: return
    for (pair in rankingList.toRankMap().keyValuesView()) {
      val rank = pair.one
      val playerId = pair.two.id

      val rewardResource = RankActivityRewardResourceSet.list().find { it.rankRange.contains(rank) } ?: continue

      val mail = Mail {
        title(RankActivitySettingConf.mailTitleId)
        content(RankActivitySettingConf.mailContentId, listOf(I18nText.Arg(rank)))
        rewards(rewardResource.rewards, RankActivityLogSource.Reward)
      }
      mailService.sendMail(playerId, mail)
    }
  }
}
