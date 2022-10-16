package play.example.game.app.module.mail

import org.springframework.stereotype.Component
import play.example.common.StatusCode
import play.example.common.id.UIDGenerator
import play.example.game.app.module.common.message.toProto
import play.example.game.app.module.common.model.I18nText
import play.example.game.app.module.mail.entity.*
import play.example.game.app.module.mail.event.PlayerCheckMailboxEvent
import play.example.game.app.module.mail.event.PlayerMailEvent
import play.example.game.app.module.player.OnlinePlayerService
import play.example.game.app.module.player.PlayerManager.Self
import play.example.game.app.module.player.condition.PlayerCondition
import play.example.game.app.module.player.condition.PlayerConditionService
import play.example.game.app.module.player.event.PlayerEventBus
import play.example.game.app.module.player.event.PlayerLoginEvent
import play.example.game.app.module.player.event.subscribe
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
  private val onlinePlayerService: OnlinePlayerService,
  private val rewardService: RewardService
) : OrderedSmartInitializingSingleton {
  private val mailCountMax = 100

  init {
    eventBus.subscribe<PlayerLoginEvent>(::onLogin)
    eventBus.subscribe<PlayerMailEvent>(::sendMail)
    eventBus.subscribe<PlayerCheckMailboxEvent>(::checkMailBox)
  }

  override fun afterSingletonsInstantiated() {
    deleteExpiredPublicMails()
  }

  private fun deleteExpiredPublicMails() {
    val expiredMails = publicMailCache.getAll().filter(::isExpired).toList()
    if (expiredMails.isNotEmpty()) {
      expiredMails.forEach(publicMailCache::delete)
      // TODO log
    }
  }

  private fun onLogin(self: Self) {
    forceDeleteTrashMails(self)
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
    eventBus.publish(PlayerMailEvent(playerId, mail))
  }

  fun sendMail(self: Self, mail: Mail) {
    val id = uidGenerator.nextId()
    val mailEntity = PlayerMailEntity(
      id, self.id, mail.title, mail.content, mail.rewards, mail.logSource, 0, mail.createTime, mail.displayTime
    )
    playerMailCache.create(mailEntity)

    Session.write(self.id, MailModule.newMailPush(1))
  }

  private fun forceDeleteTrashMails(self: Self) {
    val mails = playerMailCache.getByIndex(self.id)
    mails.sortBy { it.id }
    // 清理已读且没有奖励的邮件
    if (mails.size >= mailCountMax) {
      val it = mails.iterator()
      while (it.hasNext()) {
        val mail = it.next()
        if (mail.isRead() && (!mail.hasReward() || mail.isRewarded())) {
          playerMailCache.delete(mail)
          it.remove()
        }
      }
    }

    // 清理没有奖励的邮件
    if (mails.size >= mailCountMax) {
      val it = mails.iterator()
      while (it.hasNext()) {
        val mail = it.next()
        if (!mail.hasReward()) {
          playerMailCache.delete(mail)
          it.remove()
        }
      }
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
    onlinePlayerService.postEventToOnlinePlayers(::PlayerCheckMailboxEvent)
  }

  fun reqMailList(self: Self, start: Int, count: Int): MailListProto {
    val mails = playerMailCache.getByIndex(self.id)
    mails.sortBy { it.id }
    return MailListProto(mails.subList(start, start + count).map(::toProto))
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
      return StatusCode.ParamErr
    }
    if (!entity.hasReward()) {
      return StatusCode.ParamErr
    }
    if (entity.isRewarded()) {
      return StatusCode.RewardReceived
    }
    return rewardService.tryAndExecReward(self, entity.rewards, entity.logSource).map {
      entity.setRewarded()
      it.toProto()
    }
  }
}
