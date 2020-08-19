package play.example.game.app.module.servertask.entity

import org.eclipse.collections.impl.map.mutable.primitive.IntObjectHashMap
import play.entity.IntIdEntity
import play.entity.cache.CacheSpec
import play.entity.cache.NeverExpireEvaluator
import play.example.game.app.module.task.entity.AbstractTask

@CacheSpec(loadAllOnInit = true, neverExpire = true)
class ServerTaskEntity(id: Int) : IntIdEntity(id) {

  val tasks = IntObjectHashMap<ServerTask>()
}

class ServerTask(id: Int) : AbstractTask(id)
