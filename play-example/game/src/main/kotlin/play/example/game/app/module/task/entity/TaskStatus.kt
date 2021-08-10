package play.example.game.app.module.task.entity

/**
 * 任务状态
 *
 * @author LiangZengle
 */
object TaskStatus {

  /**
   * 已接取
   */
  const val Accepted: Byte = 0

  /**
   * 已完成
   */
  const val Finished: Byte = 1

  /**
   * 已领奖
   */
  const val Rewarded: Byte = 2
}
