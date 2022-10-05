package play.example.game.app.module.activity.base

import org.springframework.stereotype.Component
import play.example.game.app.module.activity.base.entity.ActivityEntity
import play.example.game.app.module.activity.base.entity.ActivityEntityCache
import play.example.game.app.module.activity.base.stage.ActivityStage
import play.util.collection.ConcurrentLongLongMap

/**
 *
 * @author LiangZengle
 */
@Component
class ActivityCache(private val entityCache: ActivityEntityCache) {

  private val activityStageCache = ConcurrentLongLongMap()

  fun update(activityId: Int, stage: ActivityStage) {
    activityStageCache.put(activityId.toLong(), stage.identifier.toLong())
  }

  fun getActivities(activityType: ActivityType, requireStages: Int): Sequence<ActivityEntity> {
    return activityStageCache.entries().asSequence()
      .filter { ActivityStage.contains(it.value.toInt(), requireStages) }
      .map { e -> entityCache.getOrThrow(e.key.toInt()) }
  }

  fun getActivity(activityId: Int, requireStages: Int): ActivityEntity? {
    if (!activityStageCache.containsKey(activityId.toLong())) {
      return null
    }
    val entity = entityCache.getOrNull(activityId) ?: return null
    if (!ActivityStage.contains(entity.stage.identifier, requireStages)) {
      return null
    }
    return entity
  }
}
