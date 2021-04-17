package play.example.game.module.mail.domain

import javax.inject.Inject
import javax.inject.Singleton
import play.example.common.id.GameUIDGenerator
import play.example.game.module.player.PlayerService
import play.example.game.module.player.Self
import play.inject.guice.EnableMultiBinding
import play.util.classOf

/**
 * 公共邮件接收资格验证
 *
 * @param T : ReceiverQualification
 */
@EnableMultiBinding
abstract class PublicMailReceiverQualifier<T : ReceiverQualification> {

  abstract fun qualificationType(): Class<T>

  /**
   * 判断是否可接收邮件
   */
  abstract fun isQualified(self: Self, qualification: T): Boolean
}

@Singleton
private class TrueMailReceiverQualifier : PublicMailReceiverQualifier<EmptyQualification>() {
  override fun isQualified(self: Self, qualification: EmptyQualification): Boolean {
    return true
  }

  override fun qualificationType(): Class<EmptyQualification> = EmptyQualification::class.java
}

@Singleton
private class PlayerIdMailReceiverQualifier : PublicMailReceiverQualifier<PlayerIdQualification>() {
  override fun isQualified(self: Self, qualification: PlayerIdQualification): Boolean {
    return qualification.playerIds.contains(self.id)
  }

  override fun qualificationType(): Class<PlayerIdQualification> = PlayerIdQualification::class.java
}

@Singleton
private class ServerIdReceiverQualifier : PublicMailReceiverQualifier<ServerIdQualification>() {
  override fun qualificationType(): Class<ServerIdQualification> = classOf()

  override fun isQualified(self: Self, qualification: ServerIdQualification): Boolean {
    return qualification.serverIds.contains(GameUIDGenerator.getServerId(self.id).toInt())
  }
}

@Singleton
private class PlayerCreateTimeReceiverQualifier @Inject constructor(private val playerService: PlayerService) :
  PublicMailReceiverQualifier<PlayerCreateTimeQualification>() {
  override fun qualificationType(): Class<PlayerCreateTimeQualification> = classOf()

  override fun isQualified(self: Self, qualification: PlayerCreateTimeQualification): Boolean {
    return qualification.serverIds.contains(GameUIDGenerator.getServerId(self.id).toInt()) &&
      playerService.isCreateTimeInRange(self, qualification.from, qualification.to)
  }
}
