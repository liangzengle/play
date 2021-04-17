package play.example.game.module.common.config

import org.eclipse.collections.api.list.primitive.IntList
import org.eclipse.collections.api.map.primitive.LongLongMap
import org.eclipse.collections.impl.factory.primitive.IntLists
import org.eclipse.collections.impl.factory.primitive.LongLongMaps
import play.config.AbstractConfig
import play.config.SingletonConfig
import play.config.validation.ReferTo
import play.example.game.module.mail.config.MailConfig

/**
 * 全局配置
 * @author LiangZengle
 */
@SingletonConfig
class CommonSetting : AbstractConfig() {

  // 背包满时发邮件的id
  @ReferTo(MailConfig::class)
  val bagFullMailId = 1

  val array: IntList = IntLists.mutable.empty()

  val intIntMap = emptyMap<Int, Int>()
  val intLongMap = emptyMap<Int, Long>()
  val longIntMap = emptyMap<Long, Int>()
  val longLongMap: LongLongMap = LongLongMaps.mutable.empty()
}
