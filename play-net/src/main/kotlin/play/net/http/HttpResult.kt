package play.net.http

import io.vavr.concurrent.Future

sealed class HttpResult {
  abstract val status: Int
  abstract val body: HttpEntity

  fun isErr() = !isOk()
  
  fun isOk() = HttpStatusCode.isSuccess(status)

  companion object {
    fun ok(): Strict = Strict(HttpStatusCode.OK, HttpEntity.empty)
    fun notFount(): Strict = Strict(HttpStatusCode.NOT_FOUND, HttpEntity.empty)
    fun forbidden(): Strict = Strict(HttpStatusCode.FORBIDDEN, HttpEntity.empty)
    fun internalServerError(): Strict = Strict(HttpStatusCode.INTERNAL_SERVER_ERROR, HttpEntity.empty)
    operator fun invoke(statusCode: Int): Strict = Strict(statusCode, HttpEntity.empty)
  }

  data class Strict(override val status: Int, override val body: HttpEntity) : HttpResult()

  class Lazy(val future: Future<Strict>) : HttpResult() {
    override val status: Int
      get() = if (!future.isCompleted) throw NoSuchElementException("Future Incomplete") else if (future.isSuccess) future.get().status else throw future.cause.get()
    override val body: HttpEntity
      get() = if (!future.isCompleted) throw NoSuchElementException("Future Incomplete") else if (future.isSuccess) future.get().body else throw future.cause.get()

    override fun toString(): String {
      return future.toString()
    }

  }
}

