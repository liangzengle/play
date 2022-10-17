package play.example.game.app.module.activity.base

import org.springframework.stereotype.Component
import play.entity.cache.EntityCacheManager
import play.example.common.StatusCode
import play.example.game.app.module.activity.base.entity.ActivityEntity
import play.example.game.app.module.activity.base.entity.PlayerActivityEntity
import play.example.game.app.module.activity.base.entity.PlayerActivityId
import play.example.game.app.module.activity.base.res.ActivityResourceSet
import play.example.game.app.module.activity.base.stage.ActivityStage
import play.example.game.app.module.player.PlayerManager
import play.util.control.Result2
import play.util.unsafeCast

/**
 *
 * @author LiangZengle
 */
@Component
class PlayerActivityService(
  private val activityCache: ActivityCache,
  entityCacheManager: EntityCacheManager
) {
  val entityCache = entityCacheManager.get(PlayerActivityEntity::class)

  fun getEntity(self: PlayerManager.Self, activityId: Int): PlayerActivityEntity {
    return entityCache.getOrCreate(PlayerActivityId(self.id, activityId), ::PlayerActivityEntity)
  }

  fun <R> process(
    self: PlayerManager.Self,
    activityId: Int,
    action: (ActivityEntity, PlayerActivityEntity) -> Result2<R>
  ): Result2<R> {
    return process(self, activityId, ActivityStage.Start.identifier, action)
  }

  fun <R> process(
    self: PlayerManager.Self,
    activityType: ActivityType,
    action: (ActivityEntity, PlayerActivityEntity) -> Result2<R>
  ): Result2<R> {
    return process(self, activityType, ActivityStage.Start.identifier, action)
  }

  fun <R> process(
    self: PlayerManager.Self,
    activityId: Int,
    requireStages: Int,
    action: (ActivityEntity, PlayerActivityEntity) -> Result2<R>
  ): Result2<R> {
    val resource = ActivityResourceSet.getOrNull(activityId) ?: return StatusCode.ResourceNotFound
    val activityEntity = activityCache.getActivity(activityId, requireStages)
      ?: return StatusCode.Failure // todo error code
    // TODO other check
    val playerActivityEntity = getEntity(self, activityId)
    return action(activityEntity, playerActivityEntity)
  }

  fun <R> process(
    self: PlayerManager.Self,
    activityType: ActivityType,
    requireStages: Int,
    action: (ActivityEntity, PlayerActivityEntity) -> Result2<R>
  ): Result2<R> {
    return activityCache.getActivities(activityType, requireStages).fold(StatusCode.Failure) { r, activityEntity ->
      val playerActivityEntity = getEntity(self, activityEntity.id)
      val result = action(activityEntity, playerActivityEntity)
      if (r.isOk()) r else result.unsafeCast()
    }
  }
}
