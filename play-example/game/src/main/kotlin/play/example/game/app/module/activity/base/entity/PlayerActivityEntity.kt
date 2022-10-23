package play.example.game.app.module.activity.base.entity

import org.eclipse.collections.api.factory.primitive.IntObjectMaps
import org.eclipse.collections.api.map.primitive.MutableIntObjectMap
import play.codegen.DisableCodegen
import play.example.game.app.module.player.entity.AbstractPlayerObjIdEntity
import play.example.game.app.module.player.entity.PlayerObjId
import play.example.game.app.module.task.entity.TaskData
import play.util.collection.SerializableAttributeMap

data class PlayerActivityId(override val playerId: Long, val activityId: Int) : PlayerObjId()

@DisableCodegen
class PlayerActivityEntity(id: PlayerActivityId) : AbstractPlayerObjIdEntity<PlayerActivityId>(id) {

  companion object {
    /**初始状态*/
    const val STATE_NONE = 0

    /**已初始化*/
    const val STATE_INITIALIZED = 1

    /**已结算*/
    const val STATE_BALANCED = 2
  }

  /** 活动开始时间 */
  var startTime = 0L

  /** 状态 */
  var state = STATE_NONE

  /** 活动数据 */
  var data = SerializableAttributeMap()
    private set

  fun clearData() {
    data = SerializableAttributeMap()
  }

  fun getSimpleTaskData(): MutableIntObjectMap<TaskData> {
    return data.attr(PlayerActivityDataKey.TaskData)
      .computeIfAbsent { IntObjectMaps.mutable.empty() }
  }

  fun isInitialized() = state == STATE_INITIALIZED

  fun isBalanced() = state == STATE_BALANCED

  fun setInitialized() {
    state = STATE_INITIALIZED
  }

  fun setBalanced() {
    state = STATE_BALANCED
  }
}
