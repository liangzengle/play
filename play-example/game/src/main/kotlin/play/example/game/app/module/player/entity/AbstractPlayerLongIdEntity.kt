package play.example.game.app.module.player.entity

import play.db.Merge
import play.db.mongo.Index
import play.entity.Entity
import play.entity.LongIdEntity
import play.entity.ObjId
import play.entity.ObjIdEntity
import play.entity.cache.CacheSpec
import play.entity.cache.ExpireEvaluator
import play.example.game.app.module.player.OnlinePlayerService
import play.util.unsafeCast
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

abstract class PlayerObjId : ObjId() {
  abstract val playerId: Long
}

/**
 * 所有玩家实体类的父类
 */
@CacheSpec(expireEvaluator = PlayerEntityExpireEvaluator::class)
@Merge(Merge.Strategy.All)
@Index(fields = ["id.playerId"], unique = false)
abstract class AbstractPlayerObjIdEntity<ID : PlayerObjId>(id: ID) : ObjIdEntity<ID>(id) {
  val playerId get() = id.playerId
}

/**
 * 所有玩家实体类的父类
 */
@CacheSpec(expireEvaluator = PlayerEntityExpireEvaluator::class)
@Merge(Merge.Strategy.All)
abstract class AbstractPlayerLongIdEntity(id: Long) : LongIdEntity(id) {
  val playerId get() = id
}

/**
 * 玩家数据缓存过期策略：在线玩家的数据永不过期
 */
@Singleton
@Named
class PlayerEntityExpireEvaluator @Inject constructor(private val onlinePlayerService: OnlinePlayerService) :
  ExpireEvaluator {
  override fun canExpire(entity: Entity<*>): Boolean =
    !onlinePlayerService.isOnline(entity.unsafeCast<AbstractPlayerLongIdEntity>().id)
}
