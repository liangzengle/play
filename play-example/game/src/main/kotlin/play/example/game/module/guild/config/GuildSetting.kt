package play.example.game.module.guild.config

import play.config.AbstractConfig
import play.config.SingletonConfig
import play.example.game.module.reward.model.Cost

/**
 * 工会全局配置
 * @author LiangZengle
 */
@SingletonConfig
class GuildSetting : AbstractConfig() {

  /**
   * 创建帮会消耗
   */
  val createCost = emptyList<Cost>()
}
