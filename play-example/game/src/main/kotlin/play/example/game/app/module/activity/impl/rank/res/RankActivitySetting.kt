package play.example.game.app.module.activity.impl.rank.res

import jakarta.validation.constraints.Positive
import org.eclipse.collections.api.factory.primitive.IntIntMaps
import org.eclipse.collections.api.map.primitive.IntIntMap
import play.example.game.app.module.common.res.TemplateMessageResource
import play.res.AbstractResource
import play.res.SingletonResource
import play.res.validation.constraints.ReferTo
import play.util.ranking.RankRequirement
import play.util.ranking.SimpleRankingElementLong

/**
 *
 * @author LiangZengle
 */
@SingletonResource
class RankActivitySetting : AbstractResource(), RankRequirement<SimpleRankingElementLong> {
  /**
   * 排行榜大小
   */
  val rankSize = 20

  /**
   * 排名门槛
   */
  val rankThreshold: IntIntMap = IntIntMaps.immutable.empty()

  /** 背包满时发邮件的标题id */
  @ReferTo(TemplateMessageResource::class)
  @Positive
  val mailTitleId = 1

  /** 背包满时发邮件的标题id */
  @ReferTo(TemplateMessageResource::class)
  @Positive
  val mailContentId = 2

  override fun isSatisfied(rank: Int, element: SimpleRankingElementLong): Boolean {
    return element.value >= rankThreshold.get(rank)
  }
}
