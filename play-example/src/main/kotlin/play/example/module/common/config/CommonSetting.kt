package play.example.module.common.config

import play.config.AbstractConfig
import play.config.SingletonConfig
import play.config.validation.ReferTo
import play.example.module.mail.config.MailConfig

/**
 * 全局配置
 * @author LiangZengle
 */
@SingletonConfig
class CommonSetting : AbstractConfig() {

  // 背包满时发邮件的id
  @ReferTo(MailConfig::class)
  val bagFullMailId = 1
}
