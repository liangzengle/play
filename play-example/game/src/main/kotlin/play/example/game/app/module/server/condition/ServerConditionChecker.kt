package play.example.game.app.module.server.condition

import play.util.control.Result2

/**
 *
 * @author LiangZengle
 */
interface ServerConditionChecker<T : ServerCondition> {

  fun check(condition: T): Result2<Nothing>
}

