package play.db

import play.util.concurrent.PlayFuture

/**
 *
 * @author LiangZengle
 */
interface CommandSupport<IN, OUT> {

  fun runCommand(cmd: IN): PlayFuture<out OUT>
}
