package play.mvc

import play.util.control.Result2

/**
 * 请求结果
 * @author LiangZengle
 */
sealed class RequestResult<out T> {

  data class Ok<T : Message>(val value: T) : RequestResult<T>()

  data class Code(val code: Int) : RequestResult<kotlin.Nothing>()

  object Nothing : RequestResult<Unit>()

  data class Future<T>(val future: io.vavr.concurrent.Future<RequestResult<T>>) : RequestResult<T>()

  companion object {
    operator fun invoke(result: Result2<Message>): RequestResult<Message> =
      if (result.isErr()) Code(result.getErrorCode()) else Ok(result.get())

    @JvmName("unit")
    @OverloadResolutionByLambdaReturnType
    inline operator fun invoke(f: () -> Unit): RequestResult<Unit> {
      f()
      return Nothing
    }

    @JvmName("code")
    @OverloadResolutionByLambdaReturnType
    inline operator fun invoke(f: () -> Int): RequestResult<Int> {
      return Code(f())
    }

    @JvmName("ok")
    @OverloadResolutionByLambdaReturnType
    inline operator fun invoke(f: () -> Message): RequestResult<Message> {
      return Ok(f())
    }

    @JvmName("of")
    @OverloadResolutionByLambdaReturnType
    inline operator fun invoke(f: () -> Result2<Message>): RequestResult<Message> {
      val r = f()
      return RequestResult(r)
    }

    @JvmName("async")
    @OverloadResolutionByLambdaReturnType
    inline operator fun <T> invoke(f: () -> io.vavr.concurrent.Future<RequestResult<T>>): RequestResult<T> {
      return Future(f())
    }
  }
}
