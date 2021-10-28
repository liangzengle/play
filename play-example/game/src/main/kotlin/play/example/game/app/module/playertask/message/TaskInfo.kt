package play.example.game.app.module.playertask.message

import kotlinx.serialization.Serializable

/**
 *
 * @author LiangZengle
 */
@Serializable
class TaskInfo(
  val id: Int,
  val status: Byte,
  val progresses: List<Int>
)
