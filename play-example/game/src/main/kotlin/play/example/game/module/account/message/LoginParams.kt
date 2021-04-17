package play.example.game.module.account.message

import kotlinx.serialization.Serializable

/**
 *
 * @author LiangZengle
 */
@Serializable
class LoginParams(
  val platform: String,
  val serverId: Int,
  val account: String
)
