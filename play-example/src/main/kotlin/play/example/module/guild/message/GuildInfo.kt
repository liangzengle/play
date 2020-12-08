package play.example.module.guild.message

import kotlinx.serialization.Serializable

/**
 *
 * @author LiangZengle
 */
@Serializable
data class GuildInfo(
  val id: Long,
  val name: String,
  val leaderId: Long,
  val leaderName: String,
  val membersCount: Int
)
