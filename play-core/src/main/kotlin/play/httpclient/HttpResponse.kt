package play.httpclient

import java.net.http.HttpResponse

fun <T> HttpResponse<T>.isSuccess() = statusCode() in 200..299

fun <T> HttpResponse<T>.getOrNull(): T? = body()

fun <T> HttpResponse<T>.getOrThrow(): T {
  if (!isSuccess()) {
    throw UnsuccessfulStatusCodeException(uri().toString(), statusCode())
  }
  return body() ?: throw NoSuchElementException()
}
