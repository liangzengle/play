package play.example.game.app.module.player.condition

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import play.example.game.app.module.player.Self
import play.example.game.app.module.player.event.PlayerEvent
import play.example.game.app.module.player.event.PlayerEventReceiveBuilder
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

fun PlayerEventReceiveBuilder.listenConditionEvents(
  conditions: Iterable<PlayerCondition>,
  op: (Self, PlayerEvent) -> Unit
): PlayerEventReceiveBuilder {
  conditions.asSequence().map { it.type.eventType }.distinct().forEach { match(it, op) }
  return this;
}

fun <T : Any> PlayerEventReceiveBuilder.listenConditionEvents(
  conditionsHolder: Iterable<T>,
  transform: (T) -> Iterable<PlayerCondition>,
  op: (Self, PlayerEvent) -> Unit
): PlayerEventReceiveBuilder {
  conditionsHolder.asSequence()
    .flatMap(transform)
    .map { it.type.eventType }
    .distinct()
    .forEach { match(it, op) }
  return this;
}
