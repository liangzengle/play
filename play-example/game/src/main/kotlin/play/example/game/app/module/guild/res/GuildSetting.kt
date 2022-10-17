package play.example.game.app.module.guild.res

import play.example.game.app.module.reward.model.CostList
import play.res.AbstractResource
import play.res.SingletonResource

/**
 * 工会全局配置
 * @author LiangZengle
 */
@SingletonResource
class GuildSetting : AbstractResource() {

  /**
   * 创建帮会消耗
   */
  val createCost = CostList.Empty
}
