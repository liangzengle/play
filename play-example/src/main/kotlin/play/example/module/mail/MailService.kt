package play.example.module.mail

import play.Log
import play.example.module.common.config.CommonSettingConf
import play.example.module.mail.config.MailConfigSet
import play.example.module.mail.domain.PublicMailReceiverQualifier
import play.example.module.mail.domain.ReceiverQualification
import play.example.module.mail.entity.*
import play.example.module.mail.event.PlayerMailEvent
import play.example.module.player.Self
import play.example.module.player.event.*
import play.example.module.reward.RawRewardConverter
import play.example.module.reward.model.Reward
import play.example.module.server.ServerService
import play.inject.Injector
import play.inject.guice.PostConstruct
import play.util.collection.toImmutableMap
import play.util.time.currentMillis
import play.util.unsafeCast
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MailService @Inject constructor(
  private val publicMailCache: PublicMailEntityCache,
  private val playerMailCache: PlayerMailEntityCache,
  private val eventBus: PlayerEventBus,
  private val injector: Injector,
  private val rawRewardConvert: RawRewardConverter,
  private val serverService: ServerService
) : PostConstruct, PlayerEventListener {
  private val mailCountMax = 100

  private lateinit var receiverQualifiers: Map<Class<out ReceiverQualification>, PublicMailReceiverQualifier<ReceiverQualification>>

  override fun postConstruct() {
    receiverQualifiers =
      injector.getInstancesOfType(PublicMailReceiverQualifier::class.java).toImmutableMap { it.qualificationType() }
        .unsafeCast()

    val expiredMails = publicMailCache.asSequence().filter(::isExpired).toList()
    if (expiredMails.isNotEmpty()) {
      expiredMails.forEach(publicMailCache::remove)
      // TODO log
    }
  }

  override fun playerEventReceive(): PlayerEventReceive {
    return PlayerEventReceiveBuilder()
      .match<PlayerLoginEvent>(::onLogin)
      .match<PlayerMailEvent>(::sendMail)
      .build()
  }

  private fun onLogin(self: Self) {
    checkMailBox(self)
  }

  private fun checkMailBox(self: Self) {
    publicMailCache.asSequence()
      .filter {
        !it.isReceived(self.id) &&
          receiverQualifiers[it.qualification.javaClass]?.isQualified(self, it.qualification) ?: false
      }
      .forEach { mail ->
        sendMail(
          self,
          MailBuilder(mail.title, mail.content, rawRewardConvert.toReward(self, mail.rewards), mail.logSource)
        )
      }
  }

  private fun isExpired(mail: PublicMail): Boolean {
    return mail.endTime > 0 && mail.endTime < currentMillis()
  }

  fun sendMailIfNotFull(self: Self, mailBuilder: MailBuilder) {
    val entity = playerMailCache.getOrCreate(self.id)
    if (entity.count() >= mailCountMax) {
      // TODO log
      return
    }
    sendMail(self, mailBuilder)
  }

  private fun sendMail(self: Self, event: PlayerMailEvent) = sendMail(self, event.mailBuilder)

  fun sendMail(self: Self, b: MailBuilder) {
    val entity = playerMailCache.getOrCreate(self.id)
    val mail = Mail(0, b.title, b.content, b.rewards, b.logSource, 0, b.createTime)
    entity.addMail(mail)
    // TODO
  }

  fun sendMail(self: Self, mailId: Int, rewards: List<Reward>, source: Int) {
    val mailCfgId = if (!MailConfigSet.contains(mailId)) CommonSettingConf.bagFullMailId else mailId
    if (mailCfgId != mailId) {
      Log.error { "找不到邮件模板: $mailId" }
    }
    val mailConfig = MailConfigSet(mailCfgId)
    sendMail(self, MailBuilder(mailConfig.title, mailConfig.content, rewards, source))
  }

  fun sendMail(playerId: Long, mailBuilder: MailBuilder) {
    eventBus.post(PlayerMailEvent(playerId, mailBuilder))
  }
}
