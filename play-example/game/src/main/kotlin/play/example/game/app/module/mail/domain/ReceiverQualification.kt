package play.example.game.app.module.mail.domain

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.databind.node.ObjectNode
import org.eclipse.collections.api.set.primitive.ImmutableIntSet
import org.eclipse.collections.api.set.primitive.ImmutableLongSet
import play.util.json.Json

/**
 * 领取邮件的限制
 */
sealed class ReceiverQualification {

  @Suppress("unused")
  companion object {
    @JsonCreator
    @JvmStatic
    private fun fromJson(node: ObjectNode): ReceiverQualification {
      if (node.has("playerIds")) {
        return Json.convert(node, PlayerIdQualification::class.java)
      }
      if (node.has("from") && node.has("to")) {
        return Json.convert(node, PlayerCreateTimeQualification::class.java)
      }
      if (node.has("serverIds")) {
        return Json.convert(node, ServerIdQualification::class.java)
      }
      return EmptyQualification
    }
  }
}

/**
 * 无限制
 */
object EmptyQualification : ReceiverQualification()

/**
 * 玩家id限制
 */
data class PlayerIdQualification(val playerIds: ImmutableLongSet) : ReceiverQualification()

/**
 * 服id限制
 */
data class ServerIdQualification(val serverIds: ImmutableIntSet) : ReceiverQualification()

/**
 * 在某个时间端创角的玩家
 *
 * @property serverIds 服id
 * @property from 起始时间（毫秒）
 * @property to 截至时间（毫秒）
 */
data class PlayerCreateTimeQualification(val serverIds: ImmutableIntSet, val from: Long, val to: Long) :
  ReceiverQualification()
