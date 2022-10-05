package play.example.game.app.module.reward.model

/**
 * 奖励转化结果
 *
 * @author LiangZengle
 */
sealed class TransformedResult {
  /**
   * 未改变
   */
  object Unchanged : TransformedResult()

  /**
   * 变成空奖励
   */
  object None : TransformedResult()

  /**
   * 变成另一种奖励
   * @property reward Reward
   * @constructor
   */
  class Single(val reward: Reward) : TransformedResult()

  /**
   * 变成一堆奖励
   * @property rewardList List<Reward>
   * @constructor
   */
  class Multi(val rewardList: List<Reward>) : TransformedResult()
}
