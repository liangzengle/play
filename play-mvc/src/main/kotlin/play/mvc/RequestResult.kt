package play.mvc

import play.util.concurrent.PlayFuture
import play.util.control.Result2

/**
 * 请求结果
 * @author LiangZengle
 */
sealed class RequestResult<out T> {

  data class Normal<T>(val code: Int, val value: T?): RequestResult<T>()

  object None : RequestResult<Unit>()

  data class Future<T>(val future: PlayFuture<RequestResult<T>>) : RequestResult<T>()

  companion object {

    @JvmStatic
    fun <T> ok(value: T) = Normal(0, value)

    @JvmStatic
    fun err(code: Int) = Normal(code, null)

    @JvmStatic
    @JvmName("of")
    operator fun <T> invoke(code: Int, value: T) = Normal(code, value)

    operator fun <T> invoke(result: Result2<T>): RequestResult<T> =
      if (result.isErr()) err(result.getErrorCode()) else ok(result.get())

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
