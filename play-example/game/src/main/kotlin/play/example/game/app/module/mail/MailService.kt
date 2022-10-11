package play.example.game.app.module.mail

import org.springframework.stereotype.Component
import play.example.common.StatusCode
import play.example.common.id.UIDGenerator
import play.example.game.app.module.common.message.toProto
import play.example.game.app.module.common.model.I18nText
import play.example.game.app.module.mail.entity.*
import play.example.game.app.module.mail.event.PlayerCheckMailboxEvent
import play.example.game.app.module.mail.event.PlayerMailEvent
import play.example.game.app.module.player.PlayerManager.Self
import play.example.game.app.module.player.condition.PlayerCondition
import play.example.game.app.module.player.condition.PlayerConditionService
import play.example.game.app.module.player.event.*
import play.example.game.app.module.reward.RawRewardConverter
import play.example.game.app.module.reward.RewardService
import play.example.game.app.module.reward.message.toProto
import play.example.game.app.module.reward.model.RewardList
import play.example.game.app.module.reward.res.RawReward
import play.example.game.container.net.Session
import play.example.module.mail.message.MailListProto
import play.example.module.mail.message.MailProto
import play.example.reward.message.RewardResultSetProto
import play.spring.OrderedSmartInitializingSingleton
import play.util.control.Result2
import play.util.control.map
import play.util.getOrNull
import play.util.time.Time.currentMillis

@Component
class MailService(
  private val publicMailCache: PublicMailEntityCache,
  private val playerMailCache: PlayerMailEntityCache,
  private val eventBus: PlayerEventBus,
  private val rawRewardConvert: RawRewardConverter,
  private val playerConditionService: PlayerConditionService,
  private val uidGenerator: UIDGenerator,
  private val playerEventBus: PlayerEventBus,
  private val rewardService: RewardService
) : PlayerEventListener, OrderedSmartInitializingSingleton {
  private val mailCountMax = 100

  override fun afterSingletonsInstantiated() {
    deleteExpiredPublicMails()
  }

  override fun playerEventReceive(): PlayerEventReceive {
    return PlayerEventReceiveBuilder().match<PlayerLoginEvent>(::onLogin).match(::sendMail)
      .match<PlayerCheckMailboxEvent>(::checkMailBox).build()
  }

  private fun deleteExpiredPublicMails() {
    val expiredMails = publicMailCache.getAll().filter(::isExpired).toList()
    if (expiredMails.isNotEmpty()) {
      expiredMails.forEach(publicMailCache::delete)
      // TODO log
    }
  }

  private fun onLogin(self: Self) {
    checkMailBox(self)
  }

  private fun checkMailBox(self: Self) {
    publicMailCache.getAll().filter {
      !it.isReceived(self.id) && playerConditionService.check(self, it.receiveConditions).isOk()
    }.forEach { mail ->
      sendMail(
        self,
        Mail(
          mail.title,
          mail.content,
          RewardList(rawRewardConvert.toReward(self, mail.rewards)),
          mail.logSource,
          currentMillis()
        )
      )
      mail.addReceiver(self.id)
    }
  }

  private fun isExpired(mail: PublicMailEntity): Boolean {
    return mail.expireTime > 0 && mail.expireTime < currentMillis()
  }

  private fun sendMail(self: Self, event: PlayerMailEvent) = sendMail(self, event.mail)

  fun sendMail(playerId: Long, mail: Mail) {
    eventBus.post(PlayerMailEvent(playerId, mail))
  }

  fun sendMail(self: Self, mail: Mail) {
    val id = uidGenerator.nextId()
    val mailEntity = PlayerMailEntity(
      id, self.id, mail.title, mail.content, mail.rewards, mail.logSource, 0, mail.createTime, mail.displayTime
    )
    playerMailCache.create(mailEntity)

    Session.write(self.id, MailModule.newMailPush(1))

    forceDeleteTrashMails(self)
    // TODO
  }

  private fun forceDeleteTrashMails(self: Self) {
    var deleteCount = 0
    if (playerMailCache.getIndexSize(self.id) >= mailCountMax) {
      val mails = playerMailCache.getByIndex(self.id)
      for (mail in mails) {
        if (mail.isRead() && (!mail.hasReward() || mail.isRewarded())) {
          playerMailCache.delete(mail)
          deleteCount++
        }
      }
    }

    if (playerMailCache.getIndexSize(self.id) >= mailCountMax) {
      val mails = playerMailCache.getByIndex(self.id)
      for (mail in mails) {
        if (!mail.hasReward()) {
          playerMailCache.delete(mail)
          deleteCount++
        }
      }
    }

    if (deleteCount > 0) {
      Session.write(self.id, MailModule.forceDeleteTrashMailsPush(deleteCount))
    }
  }

  fun createPublicMail(
    title: I18nText,
    content: I18nText,
    receiveConditions: List<PlayerCondition>,
    rewards: List<RawReward>,
    logSource: Int,
    startTime: Long,
    expireTime: Long
  ) {
    publicMailCache.getOrCreate(
      uidGenerator.nextId(),
      title,
      content,
      receiveConditions,
      rewards,
      logSource,
      startTime,
      expireTime,
      currentMillis()
    )
    playerEventBus.postToOnlinePlayers(::PlayerCheckMailboxEvent)
  }

  fun reqMailList(self: Self, num: Int): MailListProto {
    return MailListProto(playerMailCache.getByIndex(self.id).map(::toProto))
  }

  private fun toProto(entity: PlayerMailEntity): MailProto {
    return MailProto(
      entity.id,
      entity.title.toProto(),
      entity.content.toProto(),
      entity.rewards.toMap(),
      entity.displayTime,
      entity.status
    )
  }

  fun reqMailReward(self: Self, mailId: Long): Result2<RewardResultSetProto> {
    val entity = playerMailCache.get(mailId).getOrNull() ?: return StatusCode.Failure
    if (entity.playerId != self.id) {
      return StatusCode.Failure
    }
    if (entity.isRewarded()) {
      return StatusCode.Failure
    }
    return rewardService.tryAndExecReward(self, entity.rewards, entity.logSource).map {
      entity.setRewarded()
      it.toProto()
    }
  }
}
