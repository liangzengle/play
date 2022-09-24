package play.example.game.app.module.mail

import org.springframework.stereotype.Component
import play.Log
import play.example.common.id.UIDGenerator
import play.example.game.app.module.common.res.CommonSettingConf
import play.example.game.app.module.mail.entity.*
import play.example.game.app.module.mail.event.PlayerMailEvent
import play.example.game.app.module.mail.res.MailResourceSet
import play.example.game.app.module.player.PlayerManager.Self
import play.example.game.app.module.player.condition.PlayerConditionService
import play.example.game.app.module.player.event.*
import play.example.game.app.module.reward.RawRewardConverter
import play.example.game.app.module.reward.model.Reward
import play.example.game.container.net.Session
import play.spring.OrderedSmartInitializingSingleton
import play.util.time.Time.currentMillis

@Component
class MailService(
  private val publicMailCache: PublicMailEntityCache,
  private val playerMailCache: PlayerMailEntityCache,
  private val eventBus: PlayerEventBus,
  private val rawRewardConvert: RawRewardConverter,
  private val playerConditionService: PlayerConditionService,
  private val uidGenerator: UIDGenerator
) : PlayerEventListener, OrderedSmartInitializingSingleton {
  private val mailCountMax = 100

  override fun afterSingletonsInstantiated() {
    deleteExpiredPublicMails()
  }

  override fun playerEventReceive(): PlayerEventReceive {
    return PlayerEventReceiveBuilder()
      .match<PlayerLoginEvent>(::onLogin)
      .match<PlayerMailEvent>(::sendMail)
      .build()
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
    publicMailCache.getAll()
      .filter {
        !it.isReceived(self.id) && playerConditionService.check(self, it.receiveConditions).isOk()
      }
      .forEach { mail ->
        sendMail(
          self,
          Mail(mail.title, mail.content, rawRewardConvert.toReward(self, mail.rewards), mail.logSource)
        )
      }
  }

  private fun isExpired(mail: PublicMail): Boolean {
    return mail.endTime > 0 && mail.endTime < currentMillis()
  }

  private fun sendMail(self: Self, event: PlayerMailEvent) = sendMail(self, event.mail)

  fun sendMail(self: Self, mail: Mail) {
    val id = uidGenerator.nextId()
    val mailEntity =
      PlayerMailEntity(id, self.id, mail.title, mail.content, mail.rewards, mail.logSource, 0, mail.createTime)
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

  fun sendMail(self: Self, mailId: Int, rewards: List<Reward>, source: Int) {
    val mailCfgId = if (!MailResourceSet.contains(mailId)) CommonSettingConf.bagFullMailId else mailId
    if (mailCfgId != mailId) {
      Log.error { "找不到邮件模板: $mailId" }
    }
    val mailConfig = MailResourceSet.getOrThrow(mailCfgId)
    sendMail(self, Mail(mailConfig.title, mailConfig.content, rewards, source))
  }

  fun sendMail(playerId: Long, mail: Mail) {
    eventBus.post(PlayerMailEvent(playerId, mail))
  }
}
