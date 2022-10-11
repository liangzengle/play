package play.example.game.app.module.common.res

import jakarta.validation.constraints.Min
import org.eclipse.collections.api.map.primitive.LongLongMap
import org.eclipse.collections.impl.factory.primitive.LongLongMaps
import play.res.AbstractResource
import play.res.SingletonResource
import play.res.validation.constraints.ReferTo

/**
 * 全局配置
 * @author LiangZengle
 */
@SingletonResource
class CommonSetting : AbstractResource() {

  // 背包满时发邮件的标题id
  @ReferTo(TemplateMessageResource::class)
  @Min(1)
  val bagFullMailTitleId = 1

  // 背包满时发邮件的内容id
  @ReferTo(TemplateMessageResource::class)
  @Min(1)
  val bagFullMailContentId = 2

  val array: List<Int> = emptyList()

  val intIntMap = emptyMap<Int, Int>()
  val intLongMap = emptyMap<Int, Long>()
  val longIntMap = emptyMap<Long, Int>()
  val longLongMap: LongLongMap = LongLongMaps.mutable.empty()
}
