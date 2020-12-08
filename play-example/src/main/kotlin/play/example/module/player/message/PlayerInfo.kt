package play.example.module.player.message

import kotlinx.serialization.Serializable

/**
 *
 * @author LiangZengle
 */
@Serializable
data class PlayerInfo(val Id: Long, val name: String)
