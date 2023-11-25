package play.mvc

import com.google.common.collect.Lists
import play.util.concurrent.PlayFuture
import play.util.control.Result2

/**
 * 请求结果
 * @author LiangZengle
 */
sealed class RequestResult<out T> {

  data class Ok<T>(val value: T?) : RequestResult<T>()

  data class Err(val code: Int, val args: List<String>) : RequestResult<Nothing>()

  object None : RequestResult<Unit>()

  data class Future<T>(val future: PlayFuture<RequestResult<T>>) : RequestResult<T>()

  companion object {

    @JvmStatic
    fun <T> ok(value: T) = Ok(value)

    inline fun <T> ok(op: () -> T) = Ok(op())

    @JvmStatic
    fun err(code: Int) = Err(code, emptyList())

    @JvmStatic
    fun err(code: Int, arg0: String, vararg restArgs: String): Err {
      return if (restArgs.isEmpty()) Err(code, listOf(arg0)) else Err(code, Lists.asList(arg0, restArgs))
    }

    @JvmStatic
    fun err(code: Int, arg0: Any, vararg restArgs: Any?): Err {
      return if (restArgs.isEmpty()) Err(code, listOf(arg0.toString()))
      else Err(code, Lists.asList(arg0.toString(), Array(restArgs.size) { i -> restArgs[i]?.toString().orEmpty() }))
    }

    operator fun <T> invoke(result: Result2<T>): RequestResult<T> =
      if (result.isErr()) err(result.getCode(), result.asErr().args) else ok(result.get())

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

@Suppress("NOTHING_TO_INLINE")
inline fun <T> Result2<T>.toRequestResult() = RequestResult(this)
