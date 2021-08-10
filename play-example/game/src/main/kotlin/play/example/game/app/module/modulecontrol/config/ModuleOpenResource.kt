package play.example.game.app.module.modulecontrol.config

import play.example.game.app.module.player.condition.PlayerCondition
import play.res.AbstractResource
import javax.validation.Valid

/**
 *
 * @author LiangZengle
 */
class ModuleOpenResource : AbstractResource() {

  @Valid
  val conditions = emptyList<PlayerCondition>()
}
