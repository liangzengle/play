package play.example.game.app.module.reward.model

/**
 *
 * @author LiangZengle
 */
sealed class TransformedResult {
  object Unchanged: TransformedResult()
  object None: TransformedResult()
  class Single(val reward: Reward) : TransformedResult()
  class Multi(val rewardList: List<Reward>) : TransformedResult()
}
