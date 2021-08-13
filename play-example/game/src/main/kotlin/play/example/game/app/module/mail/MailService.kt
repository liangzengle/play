package play.example.game.app.module.mail

import org.springframework.beans.factory.BeanFactory
import play.Log
import play.example.game.app.module.common.config.CommonSettingConf
import play.example.game.app.module.mail.config.MailResourceSet
import play.example.game.app.module.mail.entity.*
import play.example.game.app.module.mail.event.PlayerMailEvent
import play.example.game.app.module.player.Self
import play.example.game.app.module.player.condition.PlayerConditionService
import play.example.game.app.module.player.event.*
import play.example.game.app.module.reward.RawRewardConverter
import play.example.game.app.module.reward.model.Reward
import play.example.game.container.net.SessionWriter
import play.spring.OrderedSmartInitializingSingleton
import play.util.time.currentMillis
import java.util.function.Predicate
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
@Named
class MailService @Inject constructor(
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
    val expiredMails = publicMailCache.asSequence().filter(::isExpired).toList()
    if (expiredMails.isNotEmpty()) {
      expiredMails.forEach(publicMailCache::delete)
      // TODO log
    }
  }

  private fun onLogin(self: Self) {
    checkMailBox(self)
  }

  private fun checkMailBox(self: Self) {
    publicMailCache.asSequence()
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

    SessionWriter.write(self.id, MailModule.newMailPush(1))

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
      SessionWriter.write(self.id, MailModule.forceDeleteTrashMailsPush(deleteCount))
    }
  }

  fun sendMail(self: Self, mailId: Int, rewards: List<Reward>, source: Int) {
    val mailCfgId = if (!MailResourceSet.contains(mailId)) CommonSettingConf.bagFullMailId else mailId
    if (mailCfgId != mailId) {
      Log.error { "找不到邮件模板: $mailId" }
    }
    val mailConfig = MailResourceSet(mailCfgId)
    sendMail(self, Mail(mailConfig.title, mailConfig.content, rewards, source))
  }

  fun sendMail(playerId: Long, mail: Mail) {
    eventBus.post(PlayerMailEvent(playerId, mail))
  }
}
