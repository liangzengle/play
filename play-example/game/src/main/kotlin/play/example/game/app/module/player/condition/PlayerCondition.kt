package play.example.game.app.module.player.condition

import com.fasterxml.jackson.databind.JsonNode
import play.example.game.app.module.player.PlayerManager.Self
import play.example.game.app.module.player.event.PlayerEvent
import play.example.game.app.module.player.event.PlayerEventBus
import play.util.json.AbstractTypeResolver
import play.util.json.JsonAbstractType

/**
 *
 * @author LiangZengle
 */
@JsonAbstractType(PlayerConditionTypeResolver::class)
abstract class PlayerCondition(@JvmField val type: PlayerConditionType)

class PlayerConditionTypeResolver : AbstractTypeResolver<PlayerCondition>() {
  override fun resolve(node: JsonNode): Class<out PlayerCondition> {
    val typeNode = node.get("type")
    return PlayerConditionType.valueOf(typeNode.textValue()).type
  }
}

fun PlayerEventBus.subscribeAll(conditions: Iterable<PlayerCondition>, op: (Self, PlayerEvent) -> Unit) {
  conditions.asSequence().map { it.type.eventType }.distinct().forEach { subscribe(it, op) }
}

fun <T : Any> PlayerEventBus.subscribeAll(
  conditionsHolder: Iterable<T>,
  transform: (T) -> Iterable<PlayerCondition>,
  op: (Self, PlayerEvent) -> Unit
) {
  conditionsHolder.asSequence().flatMap(transform).map { it.type.eventType }.distinct().forEach { subscribe(it, op) }
}
