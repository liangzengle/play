package play.example.module.player.entity

import javax.inject.Inject
import javax.inject.Singleton
import play.db.Entity
import play.db.EntityLong
import play.db.Merge
import play.db.cache.CacheSpec
import play.db.cache.ExpireEvaluator
import play.example.module.player.OnlinePlayerService
import play.util.unsafeCast

/**
 * 所有玩家实体类的父类
 */
@CacheSpec(expireEvaluator = PlayerEntityExpireEvaluator::class)
@Merge(Merge.Strategy.All)
abstract class AbstractPlayerEntity(id: Long) : EntityLong(id) {
  val playerId get() = id
}

/**
 * 玩家数据缓存过期策略：在线玩家的数据永不过期
 */
@Singleton
private class PlayerEntityExpireEvaluator @Inject constructor(private val onlinePlayerService: OnlinePlayerService) :
  ExpireEvaluator {
  override fun canExpire(entity: Entity<*>): Boolean =
    !onlinePlayerService.isOnline(entity.unsafeCast<AbstractPlayerEntity>().id)
}
