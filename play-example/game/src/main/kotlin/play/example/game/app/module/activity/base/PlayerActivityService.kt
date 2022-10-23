package play.example.game.app.module.activity.base

import org.springframework.stereotype.Component
import play.entity.cache.EntityCacheManager
import play.example.common.StatusCode
import play.example.game.app.module.activity.base.domain.ActivityErrorCode
import play.example.game.app.module.activity.base.entity.ActivityEntity
import play.example.game.app.module.activity.base.entity.PlayerActivityEntity
import play.example.game.app.module.activity.base.entity.PlayerActivityId
import play.example.game.app.module.activity.base.res.ActivityResourceSet
import play.example.game.app.module.activity.base.stage.ActivityStage
import play.example.game.app.module.player.PlayerManager
import play.example.game.app.module.player.PlayerServiceFacade
import play.spring.SingletonBeanContext
import play.util.control.Result2
import play.util.unsafeCast

/**
 *
 * @author LiangZengle
 */
@Component
class PlayerActivityService(
  private val activityCache: ActivityCache,
  entityCacheManager: EntityCacheManager,
  private val beanContext: SingletonBeanContext,
  private val playerServiceFacade: PlayerServiceFacade
) {
  val entityCache = entityCacheManager.get(PlayerActivityEntity::class)

  fun getEntity(self: PlayerManager.Self, activityEntity: ActivityEntity): PlayerActivityEntity? {
    if (activityEntity.startTime <= 0) {
      return null
    }
    val activityId = activityEntity.id
    val entityId = PlayerActivityId(self.id, activityId)
    val playerActivityEntity: PlayerActivityEntity? = if (activityEntity.stage == ActivityStage.Start) {
      entityCache.getOrCreate(entityId, ::PlayerActivityEntity)
    } else {
      entityCache.getOrNull(entityId)
    }
    if (playerActivityEntity == null) {
      return null
    }
    if (playerActivityEntity.startTime != activityEntity.startTime) {
      // 活动结算
    }
    return playerActivityEntity
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
      ?: return ActivityErrorCode.ActivityClosed
    val joinCheckResult = playerServiceFacade.checkConditions(self, resource.joinConditions)
    if (joinCheckResult.isErr()) {
      return joinCheckResult
    }
    val playerActivityEntity = getEntity(self, activityEntity)
    return action(activityEntity, playerActivityEntity!!)
  }

  fun <R> process(
    self: PlayerManager.Self,
    activityType: ActivityType,
    requireStages: Int,
    action: (ActivityEntity, PlayerActivityEntity) -> Result2<R>
  ): Result2<R> {
    return activityCache.getActivities(activityType, requireStages).fold(StatusCode.Failure) { r, activityEntity ->
      val result = process(self, activityEntity.id, requireStages, action)
      if (r.isOk()) r else result.unsafeCast()
    }
  }
}
