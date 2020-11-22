package play.example.module.reward

import org.junit.jupiter.api.Assertions
import play.example.module.reward.model.Cost
import play.example.module.reward.model.ItemReward

/**
 *
 * @author LiangZengle
 */
internal class RewardHelperTest {

  @org.junit.jupiter.api.Test
  fun mergeReward() {
    val rewards = listOf(
      ItemReward(1, 1),
      ItemReward(1, 1)
    )
    val mergeReward = RewardHelper.mergeReward(rewards)
    Assertions.assertTrue(mergeReward.size == 1)
    Assertions.assertEquals(mergeReward[0].num, 2)
  }

  @org.junit.jupiter.api.Test
  fun mergeCost() {
    val rewards = listOf(
      Cost(ItemReward(1, 1)),
      Cost(ItemReward(1, 1))
    )
    val mergedCosts = RewardHelper.mergeCost(rewards)
    Assertions.assertTrue(mergedCosts.size == 1)
    Assertions.assertEquals(mergedCosts[0].num, 2)
  }
}
