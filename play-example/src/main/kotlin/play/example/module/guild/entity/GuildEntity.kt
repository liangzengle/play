package play.example.module.guild.entity

import play.db.EntityLong
import play.db.Merge
import play.db.cache.CacheSpec
import play.db.cache.NeverExpireEvaluator
import play.example.module.guild.message.GuildProto
import play.util.collection.ConcurrentHashSet

/**
 * 工会实体类
 * @author LiangZengle
 */
@Merge(Merge.Strategy.All)
@CacheSpec(loadAllOnInit = true, expireEvaluator = NeverExpireEvaluator::class)
class GuildEntity(id: Long, val name: String) : EntityLong(id) {

  var leaderId = 0L

  var leaderName = ""

  val members: MutableSet<Long> = ConcurrentHashSet()

  fun toProto(): GuildProto {
    return GuildProto(id, name, leaderId, leaderName, members.size)
  }
}
