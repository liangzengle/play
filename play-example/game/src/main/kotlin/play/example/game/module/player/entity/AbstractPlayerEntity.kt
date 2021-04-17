package play.example.game.module.player.entity

import javax.inject.Inject
import javax.inject.Singleton
import play.db.Merge
import play.entity.Entity
import play.entity.EntityLong
import play.entity.cache.CacheSpec
import play.entity.cache.ExpireEvaluator
import play.example.game.module.player.OnlinePlayerService
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
