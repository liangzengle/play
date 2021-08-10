package play.example.game.app.module.player.message

import kotlinx.serialization.Serializable

/**
 *
 * @author LiangZengle
 */
@Serializable
data class PlayerDTO(val Id: Long, val name: String)
