package play.example.game.app.module.servertask.entity

import org.eclipse.collections.impl.map.mutable.primitive.IntObjectHashMap
import play.entity.IntIdEntity
import play.entity.cache.CacheSpec
import play.entity.cache.NeverExpireEvaluator
import play.example.game.app.module.task.entity.AbstractTask

@CacheSpec(initialSize = CacheSpec.SIZE_ONE, loadAllOnInit = true, expireEvaluator = NeverExpireEvaluator::class)
class ServerTaskEntity(id: Int) : IntIdEntity(id) {

  val tasks = IntObjectHashMap<ServerTask>()
}

class ServerTask(id: Int) : AbstractTask(id)
