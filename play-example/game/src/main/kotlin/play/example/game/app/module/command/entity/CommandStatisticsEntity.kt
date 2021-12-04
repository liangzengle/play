package play.example.game.app.module.command.entity

import play.entity.IntIdEntity
import play.entity.cache.CacheSpec
import play.entity.cache.InitialCacheSize
import play.entity.cache.NeverExpireEvaluator

/**
 * gm指令使用情况统计
 * @author LiangZengle
 */
@CacheSpec(neverExpire = true)
@InitialCacheSize(InitialCacheSize.ONE)
class CommandStatisticsEntity(id: Int) : IntIdEntity(id) {

  val statistics: MutableMap<CommandId, Int> = HashMap()
}

data class CommandId(val module: String, val name: String)
