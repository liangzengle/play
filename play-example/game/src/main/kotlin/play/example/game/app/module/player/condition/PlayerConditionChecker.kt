package play.example.game.app.module.player.condition

import play.example.common.StatusCode
import play.example.game.app.module.player.Self
import play.inject.PlayInjector
import play.util.classOf
import play.util.collection.toImmutableMap
import play.util.control.Result2
import play.util.unsafeLazy
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 *
 * @author LiangZengle
 */
abstract class PlayerConditionChecker<T>(@JvmField val type: PlayerConditionType) {

  abstract fun check(self: Self, condition: T): Result2<Nothing>
}

@Singleton
@Named
class PlayerConditionService @Inject constructor(injector: PlayInjector) {
  private val checkerMap by unsafeLazy {
    injector.getInstancesOfType(classOf<PlayerConditionChecker<PlayerCondition>>()).toImmutableMap { it.type }
  }

  fun check(self: Self, condition: PlayerCondition): Result2<Nothing> {
    val checker = checkerMap[condition.type] ?: return StatusCode.Failure
    return checker.check(self, condition)
  }
}
