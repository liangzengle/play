package play.example.game.app.module.guild.entity

import play.db.Merge
import play.entity.LongIdEntity
import play.entity.cache.CacheSpec
import play.example.module.guild.message.GuildProto
import play.util.collection.ConcurrentHashSet

/**
 * 工会实体类
 * @author LiangZengle
 */
@Merge(Merge.Strategy.All)
@CacheSpec(loadAllOnInit = true, neverExpire = true)
class GuildEntity(id: Long, val name: String) : LongIdEntity(id) {

  var leaderId = 0L

  var leaderName = ""

  val members: MutableSet<Long> = ConcurrentHashSet()

  fun toMessage(): GuildProto {
    return GuildProto(id, name, leaderId, leaderName, members.size)
  }
}
