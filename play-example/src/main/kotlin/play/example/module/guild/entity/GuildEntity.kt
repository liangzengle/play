package play.example.module.guild.entity

import play.db.EntityLong
import play.db.Merge
import play.db.cache.CacheSpec
import play.db.cache.NeverExpireEvaluator

/**
 * 工会实体类
 * @author LiangZengle
 */
@Merge(Merge.Strategy.All)
@CacheSpec(loadAllOnInit = true, expireEvaluator = NeverExpireEvaluator::class)
class GuildEntity(id: Long, val name: String) : EntityLong(id) {

  var leaderId = 0L

  var leaderName = ""
}
