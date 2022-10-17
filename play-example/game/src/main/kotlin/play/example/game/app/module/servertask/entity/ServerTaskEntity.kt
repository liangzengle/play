package play.example.game.app.module.servertask.entity

import org.eclipse.collections.impl.map.mutable.primitive.IntObjectHashMap
import play.entity.IntIdEntity
import play.entity.cache.CacheSpec
import play.example.game.app.module.task.entity.TaskData

@CacheSpec(loadAllOnInit = true, neverExpire = true)
class ServerTaskEntity(id: Int) : IntIdEntity(id) {

  val tasks = IntObjectHashMap<TaskData>()
}
