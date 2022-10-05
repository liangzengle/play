package play.example.game.app.module.server.condition

import com.fasterxml.jackson.annotation.JsonTypeName
import org.springframework.stereotype.Component
import play.example.common.StatusCode
import play.example.game.app.module.server.ServerService
import play.res.support.CompareOperator
import play.util.control.Result2

/**
 * 开服天数条件
 * @author LiangZengle
 */
@JsonTypeName("serverOpenDay")
class ServerOpenDayCondition(val days: Int, val cmp: CompareOperator) : ServerCondition()


@Component
class ServerOpenDayConditionChecker(private val serverService: ServerService) :
  ServerConditionChecker<ServerOpenDayCondition> {
  override fun check(condition: ServerOpenDayCondition): Result2<Nothing> {
    return if (condition.cmp.check(condition.days, serverService.getServerOpenDays())) {
      StatusCode.Success
    } else StatusCode.Failure
  }
}
