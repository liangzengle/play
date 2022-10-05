package play.example.game.app.module.modulecontrol.res

import jakarta.validation.Valid
import play.example.game.app.module.player.condition.PlayerCondition
import play.res.AbstractResource
import play.res.validation.constraints.Enumerated

/**
 *
 * @author LiangZengle
 */
@Enumerated<ModuleType>(ModuleType::class)
class ModuleControlResource : AbstractResource() {

  @Valid
  val conditions = emptyList<PlayerCondition>()
}
