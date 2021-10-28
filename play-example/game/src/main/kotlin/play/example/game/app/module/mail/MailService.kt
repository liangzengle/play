package play.example.game.app.module.mail

import org.springframework.beans.factory.BeanFactory
import org.springframework.stereotype.Component
import play.Log
import play.example.game.app.module.common.res.CommonSettingConf
import play.example.game.app.module.mail.entity.*
import play.example.game.app.module.mail.event.PlayerMailEvent
import play.example.game.app.module.mail.res.MailResourceSet
import play.example.game.app.module.player.Self
import play.example.game.app.module.player.condition.PlayerConditionService
import play.example.game.app.module.player.event.*
import play.example.game.app.module.reward.RawRewardConverter
import play.example.game.app.module.reward.model.Reward
import play.example.game.container.net.Session
import play.spring.OrderedSmartInitializingSingleton
import play.util.time.Time.currentMillis
import java.util.function.Predicate

@Component
class MailService(
  private val publicMailCache: PublicMailEntityCache,
  private val playerMailIdEntityCache: PlayerMailIdEntityCache,
  private val playerMailCache: PlayerMailEntityCache,
  private val eventBus: PlayerEventBus,
  private val rawRewardConvert: RawRewardConverter,
  private val playerConditionService: PlayerConditionService
) : PlayerEventListener, OrderedSmartInitializingSingleton {
  private val mailCountMax = 100

  override fun afterSingletonsInstantiated(beanFactory: BeanFactory) {
    deleteExpiredPublicMails()
  }

  override fun playerEventReceive(): PlayerEventReceive {
    return PlayerEventReceiveBuilder()
      .match<PlayerLoginEvent>(::onLogin)
      .match<PlayerMailEvent>(::sendMail)
      .build()
  }

  private fun deleteExpiredPublicMails() {
    val expiredMails = publicMailCache.getCachedEntities().filter(::isExpired).toList()
    if (expiredMails.isNotEmpty()) {
      expiredMails.forEach(publicMailCache::delete)
      // TODO log
    }
  }

  private fun onLogin(self: Self) {
    checkMailBox(self)
  }

  private fun checkMailBox(self: Self) {
    publicMailCache.getCachedEntities()
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
    val playerMailIdEntity = playerMailIdEntityCache.getOrCreate(self.id)
    val mailId = playerMailIdEntity.nextMailId()
    val id = PlayerMailId(self.id, mailId)
    val mailEntity = PlayerMailEntity(id, mail.title, mail.content, mail.rewards, mail.logSource, 0, mail.createTime)
    playerMailCache.create(mailEntity)
    playerMailIdEntity.add(mailId)

    Session.write(self.id, MailModule.newMailPush(1))

    forceDeleteTrashMails(self)
    // TODO
  }

  private fun forceDeleteTrashMails(self: Self) {
    val playerMailIdEntity = playerMailIdEntityCache.getOrCreate(self.id)
    var deleteCount = 0
    fun delete(canRemove: Predicate<PlayerMailEntity>): Int {
      val ids = playerMailIdEntity.getMailIds().collect { PlayerMailId(self.id, it) }
      val allMails = playerMailCache.getAll(ids)
      var n = 0
      for (mail in allMails) {
        if (canRemove.test(mail)) {
          playerMailCache.delete(mail)
          playerMailIdEntity.remove(mail.id.mailId)
          n++
          // TODO log
        }
      }
      return n
    }

    if (playerMailIdEntity.count() > mailCountMax) {
      deleteCount += delete { it.isRead() && !it.hasReward() }
    }
    if (playerMailIdEntity.count() > mailCountMax) {
      deleteCount += delete { !it.hasReward() }
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
