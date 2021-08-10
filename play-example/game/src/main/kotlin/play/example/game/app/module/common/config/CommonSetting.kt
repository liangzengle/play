package play.example.game.app.module.common.config

import org.eclipse.collections.api.list.primitive.IntList
import org.eclipse.collections.api.map.primitive.LongLongMap
import org.eclipse.collections.impl.factory.primitive.IntLists
import org.eclipse.collections.impl.factory.primitive.LongLongMaps
import play.example.game.app.module.mail.config.MailResource
import play.res.AbstractResource
import play.res.SingletonResource
import play.res.validation.ReferTo

/**
 * 全局配置
 * @author LiangZengle
 */
@SingletonResource
class CommonSetting : AbstractResource() {

  // 背包满时发邮件的id
  @ReferTo(MailResource::class)
  val bagFullMailId = 1

  val array: IntList = IntLists.mutable.empty()

  val intIntMap = emptyMap<Int, Int>()
  val intLongMap = emptyMap<Int, Long>()
  val longIntMap = emptyMap<Long, Int>()
  val longLongMap: LongLongMap = LongLongMaps.mutable.empty()
}
