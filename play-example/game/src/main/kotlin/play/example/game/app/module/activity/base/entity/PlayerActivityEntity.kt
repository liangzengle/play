package play.example.game.app.module.activity.base.entity

import play.example.game.app.module.player.entity.AbstractIndexedPlayerEntity
import play.example.game.app.module.player.entity.PlayerObjId
import play.util.collection.SerializableAttributeMap

data class PlayerActivityId(override val playerId: Long, val activityId: Int) : PlayerObjId()

class PlayerActivityEntity(id: PlayerActivityId) : AbstractIndexedPlayerEntity<PlayerActivityId>(id) {

  val data = SerializableAttributeMap()
}
