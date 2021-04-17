package play

import com.typesafe.config.Config

/**
 * Created by LiangZengle on 2020/2/16.
 */
interface ApplicationLoader {

  fun load(ctxt: Context): Application

  data class Context(
    val conf: Config,
    val shutdownCoordinator: ShutdownCoordinator
  )
}
