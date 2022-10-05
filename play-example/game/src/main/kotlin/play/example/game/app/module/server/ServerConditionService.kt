package play.example.game.app.module.server

import org.springframework.stereotype.Component
import play.example.common.StatusCode
import play.example.game.app.module.server.condition.ServerCondition
import play.example.game.app.module.server.condition.ServerConditionChecker
import play.util.collection.toImmutableMap
import play.util.control.Result2
import play.util.reflect.Reflect
import play.util.unsafeCast

@Component
class ServerConditionService(checker: List<ServerConditionChecker<out ServerCondition>>) {
  private val checkerMap = checker.toImmutableMap(
    { Reflect.getTypeArg(it.javaClass, ServerConditionChecker::class.java, 0) },
    { it.unsafeCast<ServerConditionChecker<ServerCondition>>() }
  )

  fun check(condition: ServerCondition): Result2<Nothing> {
    return checkerMap[condition.javaClass]?.check(condition) ?: StatusCode.Failure
  }

  fun check(conditions: Collection<ServerCondition>): Result2<Nothing> {
    for (condition in conditions) {
      val result = checkerMap[condition.javaClass]?.check(condition) ?: StatusCode.Failure
      if (result.isErr()) {
        return result
      }
    }
    return StatusCode.Success
  }
}
