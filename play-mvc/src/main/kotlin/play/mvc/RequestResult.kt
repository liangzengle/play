package play.mvc

import play.util.concurrent.PlayFuture
import play.util.control.Result2

/**
 * 请求结果
 * @author LiangZengle
 */
sealed class RequestResult<out T> {

  data class Ok<T>(val value: T) : RequestResult<T>()

  data class Code(val code: Int) : RequestResult<Nothing>()

  object None : RequestResult<Unit>()

  data class Future<T>(val future: PlayFuture<RequestResult<T>>) : RequestResult<T>()

  companion object {

    operator fun <T> invoke(result: Result2<T>): RequestResult<T> =
      if (result.isErr()) Code(result.getErrorCode()) else Ok(result.get())

    inline fun <T : Any> async(f: () -> PlayFuture<Result2<T>>): RequestResult<T> =
      Future(f().map { RequestResult(it) })

    inline fun noResponse(f: () -> Unit): RequestResult<Unit> {
      f()
      return None
    }

    inline operator fun <T> invoke(f: () -> Result2<T>): RequestResult<T> {
      return RequestResult(f())
    }
  }
}
