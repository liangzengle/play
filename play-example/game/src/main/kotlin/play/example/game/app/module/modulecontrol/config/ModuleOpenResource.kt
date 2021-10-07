package play.example.game.app.module.modulecontrol.config

import jakarta.validation.Valid
import play.example.game.app.module.player.condition.PlayerCondition
import play.res.AbstractResource

/**
 *
 * @author LiangZengle
 */
class ModuleOpenResource : AbstractResource() {

  @Valid
  val conditions = emptyList<PlayerCondition>()
}
