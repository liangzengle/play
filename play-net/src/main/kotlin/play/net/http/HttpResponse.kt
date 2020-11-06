package play.net.http

import java.net.http.HttpResponse

fun <T> HttpResponse<T>.isSuccess() = HttpStatusCode.isSuccess(statusCode())

fun <T> HttpResponse<T>.getOrNull(): T? = body()

fun <T> HttpResponse<T>.getOrThrow(): T {
  if (!isSuccess()) {
    throw UnsuccessfulStatusCodeException(statusCode())
  }
  return body() ?: throw NoSuchElementException()
}
