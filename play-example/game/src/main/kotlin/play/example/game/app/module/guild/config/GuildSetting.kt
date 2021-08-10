package play.example.game.app.module.guild.config

import play.example.game.app.module.reward.model.Cost
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
  val createCost = emptyList<Cost>()
}
