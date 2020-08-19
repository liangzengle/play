package play.example.module.mail.domain

import play.example.module.player.Self
import javax.inject.Singleton

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
private class PlayerSetMailReceiverQualifier : PublicMailReceiverQualifier<PlayerSetQualification>() {
  override fun isQualified(self: Self, qualification: PlayerSetQualification): Boolean {
    return qualification.playerIds.contains(self.id)
  }

  override fun qualificationType(): Class<PlayerSetQualification> = PlayerSetQualification::class.java
}
