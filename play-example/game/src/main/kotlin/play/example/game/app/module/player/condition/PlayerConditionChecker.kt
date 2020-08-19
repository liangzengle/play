package play.example.game.app.module.player.condition

import org.springframework.stereotype.Component
import play.example.common.StatusCode
import play.example.game.app.module.player.PlayerManager.Self
import play.util.collection.toImmutableMap
import play.util.control.Result2
import play.util.reflect.Reflect
import play.util.unsafeCast

/**
 *
 * @author LiangZengle
 */
abstract class PlayerConditionChecker<T>(@JvmField val type: PlayerConditionType) {

  init {
    Reflect.checkTypeArgEquals(this.javaClass, PlayerConditionChecker::class.java, 0, type.type)
  }

  abstract fun check(self: Self, condition: T): Result2<Nothing>
}

@Component
class PlayerConditionService(checkers: List<PlayerConditionChecker<out PlayerCondition>>) {
  private val checkerMap =
    checkers.toImmutableMap({ it.type }, { it.unsafeCast<PlayerConditionChecker<PlayerCondition>>() })

  fun check(self: Self, condition: PlayerCondition): Result2<Nothing> {
    val checker = checkerMap[condition.type] ?: return StatusCode.Failure
    return checker.check(self, condition)
  }

  fun check(self: Self, conditions: Collection<PlayerCondition>?): Result2<Nothing> {
    if (conditions.isNullOrEmpty()) {
      return StatusCode.Success
    }
    for (condition in conditions) {
      val result = check(self, condition)
      if (result.isErr()) {
        return result
      }
    }
    return StatusCode.Success
  }
}
