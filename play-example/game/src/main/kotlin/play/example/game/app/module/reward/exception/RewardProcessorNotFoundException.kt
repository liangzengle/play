package play.example.game.app.module.reward.exception

import play.example.game.app.module.reward.model.Reward

/**
 * 找不到对应的奖励处理器异常
 * @author LiangZengle
 */
class RewardProcessorNotFoundException(reward: Reward) : RuntimeException("找不到对应的奖励处理器: $reward")
