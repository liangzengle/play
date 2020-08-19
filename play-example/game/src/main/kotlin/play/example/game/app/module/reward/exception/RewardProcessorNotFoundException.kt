package play.example.game.app.module.reward.exception

import play.example.game.app.module.reward.model.RewardType

/**
 * 找不到对应的奖励处理器异常
 * @author LiangZengle
 */
class RewardProcessorNotFoundException(type: RewardType) : RuntimeException("找不到对应的奖励处理器: $type")
