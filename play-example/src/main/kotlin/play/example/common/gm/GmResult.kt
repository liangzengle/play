package play.example.common.gm

import kotlinx.serialization.Serializable

/**
 *
 * @author LiangZengle
 */
@Serializable
class GmResult(val success: Boolean, val message: String)
