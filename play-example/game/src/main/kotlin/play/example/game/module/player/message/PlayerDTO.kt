package play.example.game.module.player.message

import kotlinx.serialization.Serializable

/**
 *
 * @author LiangZengle
 */
@Serializable
data class PlayerDTO(val Id: Long, val name: String)
