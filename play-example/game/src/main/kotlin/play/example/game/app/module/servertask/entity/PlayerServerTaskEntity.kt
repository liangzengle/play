package play.example.game.app.module.servertask.entity

import org.eclipse.collections.impl.factory.primitive.IntSets
import play.example.game.app.module.player.entity.AbstractPlayerLongIdEntity

class PlayerServerTaskEntity(id: Long) : AbstractPlayerLongIdEntity(id) {

  /**
   * 已领奖的任务id
   */
  val rewardedTaskIds = IntSets.mutable.empty()
}
