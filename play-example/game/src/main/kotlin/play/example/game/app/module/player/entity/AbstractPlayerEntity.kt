package play.example.game.app.module.player.entity

import org.springframework.stereotype.Component
import play.db.Merge
import play.entity.Entity
import play.entity.LongIdEntity
import play.entity.ObjId
import play.entity.ObjIdEntity
import play.entity.cache.CacheSpec
import play.entity.cache.ExpireEvaluator
import play.entity.cache.ShouldSpecifyInitialCacheSize
import play.example.game.app.module.player.OnlinePlayerService
import play.util.unsafeCast

abstract class PlayerObjId : ObjId() {
  abstract val playerId: Long
}

sealed interface PlayerEntityLike {
  val playerId: Long
}

/**
 * 1:1的玩家数据
 */
@CacheSpec(expireEvaluator = PlayerEntityExpireEvaluator::class)
@Merge(Merge.Strategy.All)
abstract class AbstractPlayerEntity(id: Long) : LongIdEntity(id), PlayerEntityLike {
  override val playerId get() = id
}

/**
 * 1:n的玩家数据
 *
 * @param ID : PlayerObjId
 * @property playerId Long
 * @constructor
 */
@CacheSpec(expireEvaluator = PlayerEntityExpireEvaluator::class)
@Merge(Merge.Strategy.All)
@ShouldSpecifyInitialCacheSize
abstract class AbstractPlayerObjIdEntity<ID : PlayerObjId>(id: ID) : ObjIdEntity<ID>(id), PlayerEntityLike {
  override val playerId: Long get() = id.playerId
}

/**
 * 玩家数据缓存过期策略：在线玩家的数据永不过期
 */
@Component
class PlayerEntityExpireEvaluator(private val onlinePlayerService: OnlinePlayerService) :
  ExpireEvaluator {
  override fun canExpire(entity: Entity<*>): Boolean =
    !onlinePlayerService.isOnline(entity.unsafeCast<PlayerEntityLike>().playerId)
}
