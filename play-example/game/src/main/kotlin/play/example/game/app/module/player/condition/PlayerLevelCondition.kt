package play.example.game.app.module.player.condition

import org.springframework.stereotype.Component
import play.example.game.app.module.player.PlayerManager.Self
import play.example.game.app.module.player.PlayerService
import play.example.game.app.module.player.domain.PlayerErrorCode
import play.util.control.Result2
import play.util.control.ok

/**
 *
 * @author LiangZengle
 */
data class PlayerLevelCondition(val min: Int, val max: Int) : PlayerCondition(PlayerConditionType.Level)

/**
 *
 * @author LiangZengle
 */
@Component
class PlayerLevelConditionChecker(private val playerService: PlayerService) :
  PlayerConditionChecker<PlayerLevelCondition>(PlayerConditionType.Level) {

  override fun check(self: Self, condition: PlayerLevelCondition): Result2<Nothing> {
    val playerLevel = playerService.getPlayerLevel(self.id)
    if (condition.min > playerLevel) {
      return PlayerErrorCode.Failure
    }
    if (condition.max in 1 ..< playerLevel) {
      return PlayerErrorCode.LevelNotEnough
    }
    return ok()
  }
}

