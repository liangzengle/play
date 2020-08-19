package play.example.game.app.module.reward.model

/**
 * 背包满时的处理策略
 */
enum class BagFullStrategy {

  /**
   * 背包满则拒绝
   */
  Reject,

  /**
   * 背包满则邮寄
   */
  Mail,

  /**
   * 背包满且邮箱未满时邮寄
   */
  TryMail;
}
