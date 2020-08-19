package play.example.common

import play.SystemProps

/**
 *
 * @author LiangZengle
 */
object Env {

  @JvmStatic
  fun isDev() = SystemProps.getBoolean("play.env.mode")
}
