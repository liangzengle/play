package play.example.module.mail.domain

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.databind.node.ObjectNode
import org.eclipse.collections.api.set.primitive.ImmutableIntSet
import org.eclipse.collections.api.set.primitive.ImmutableLongSet
import org.eclipse.collections.impl.factory.primitive.LongSets
import play.example.module.server.config.ServerConfig
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

    /**
     * 本服所有人
     */
    @JvmStatic
    fun ofCurrentServerPlayers() = ServerIdQualification(ServerConfig.serverIds)

    /**
     * 特定id的玩家
     */
    @JvmStatic
    fun ofPlayers(playerIds: Collection<Long>) = PlayerIdQualification(LongSets.immutable.ofAll(playerIds))

    /**
     * 特定id的玩家
     */
    @JvmStatic
    fun ofCreateTime(from: Long, to: Long) = PlayerCreateTimeQualification(ServerConfig.serverIds, from, to)
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
