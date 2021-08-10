package play.example.game.app.module.player.condition

import play.example.game.app.module.player.PlayerService
import play.example.game.app.module.player.Self
import play.example.game.app.module.player.domain.PlayerErrorCode
import play.util.control.Result2
import play.util.control.ok
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 *
 * @author LiangZengle
 */
data class PlayerLevelCondition(val min: Int, val max: Int) : PlayerCondition(PlayerConditionType.Level)

/**
 *
 * @author LiangZengle
 */
@Singleton
@Named
class PlayerLevelConditionChecker @Inject constructor(private val playerService: PlayerService) :
  PlayerConditionChecker<PlayerLevelCondition>(PlayerConditionType.Level) {

  override fun check(self: Self, condition: PlayerLevelCondition): Result2<Nothing> {
    val playerLevel = playerService.getPlayerLevel(self.id)
    if (condition.min > playerLevel) {
      return PlayerErrorCode.Failure
    }
    if (condition.max in 1 until playerLevel) {
      return PlayerErrorCode.LevelNotEnough
    }
    return ok()
  }
}

