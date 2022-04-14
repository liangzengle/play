package play.net

import play.util.concurrent.PlayFuture

/**
 * Created by LiangZengle on 2020/2/20.
 */
interface NetServer {

  fun start() {
    startAsync().await()
  }

  fun stop() {
    stopAsync().await()
  }

  fun startAsync(): PlayFuture<*>

  fun stopAsync(): PlayFuture<*>
}
