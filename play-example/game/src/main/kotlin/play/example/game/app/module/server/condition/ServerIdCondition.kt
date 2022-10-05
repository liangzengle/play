package play.example.game.app.module.server.condition

import com.fasterxml.jackson.annotation.JsonTypeName
import org.springframework.stereotype.Component
import play.example.common.StatusCode
import play.example.game.container.gs.domain.GameServerId
import play.util.control.Result2

/**
 *
 * @author LiangZengle
 */
@JsonTypeName("serverId")
class ServerIdCondition : ServerCondition() {
  val include = emptySet<Int>()
  val exclude = emptySet<Int>()
}

@Component
class ServerIdInclusiveConditionChecker(private val id: GameServerId) :
  ServerConditionChecker<ServerIdCondition> {

  override fun check(condition: ServerIdCondition): Result2<Nothing> {
    // 指定包含其余排除
    if (condition.include.isNotEmpty() && condition.include.contains(id.toInt())) {
      return StatusCode.Success
    }
    // 指定排除其余包含
    if (condition.exclude.isNotEmpty()) {
      return if (condition.exclude.contains(id.toInt())) StatusCode.Failure else StatusCode.Success
    }
    return StatusCode.Failure
  }
}
