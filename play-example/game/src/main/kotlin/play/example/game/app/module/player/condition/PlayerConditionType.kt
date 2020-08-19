package play.example.game.app.module.player.condition

import play.example.game.app.module.player.event.PlayerEvent
import play.example.game.app.module.player.event.PlayerLevelUpEvent

/**
 *
 * @author LiangZengle
 */
enum class PlayerConditionType(val type: Class<out PlayerCondition>, val eventType: Class<out PlayerEvent>) {
  Level(PlayerLevelCondition::class.java, PlayerLevelUpEvent::class.java);
}
