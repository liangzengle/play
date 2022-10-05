package play.example.game.app.module.activity.base.entity

import org.springframework.stereotype.Component
import play.entity.Entity
import play.entity.cache.ExpireEvaluator
import play.example.game.app.module.activity.base.stage.ActivityStage

/**
 *
 * @author LiangZengle
 */
@Component
class ActivityEntityExpireEvaluator : ExpireEvaluator {
  override fun canExpire(entity: Entity<*>): Boolean {
    return entity is ActivityEntity && entity.stage == ActivityStage.Close
  }
}
