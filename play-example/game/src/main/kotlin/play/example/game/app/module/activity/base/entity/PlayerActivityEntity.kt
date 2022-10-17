package play.example.game.app.module.activity.base.entity

import org.eclipse.collections.api.factory.primitive.IntObjectMaps
import org.eclipse.collections.api.map.primitive.MutableIntObjectMap
import play.codegen.DisableCodegen
import play.example.game.app.module.activity.base.PlayerActivityDataKey
import play.example.game.app.module.player.entity.AbstractPlayerObjIdEntity
import play.example.game.app.module.player.entity.PlayerObjId
import play.example.game.app.module.task.entity.TaskData
import play.util.collection.SerializableAttributeMap

data class PlayerActivityId(override val playerId: Long, val activityId: Int) : PlayerObjId()

@DisableCodegen
class PlayerActivityEntity(id: PlayerActivityId) : AbstractPlayerObjIdEntity<PlayerActivityId>(id) {

  /** 活动开始时间 */
  var startTime = 0L

  /** 活动开始时间 */
  var state = 0

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
}
