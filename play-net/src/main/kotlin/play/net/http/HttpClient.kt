package play.net.http

import io.vavr.concurrent.Future
import io.vavr.control.Try
import play.getLogger
import play.util.collection.mkString
import play.util.concurrent.toFuture
import play.util.exception.isFatal
import java.net.URI
import java.net.URLEncoder
import java.net.http.*
import java.net.http.HttpClient
import java.nio.ByteBuffer
import java.time.Duration
import java.util.concurrent.CompletionException
import java.util.concurrent.Flow
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * Created by LiangZengle on 2020/2/20.
 */
@Singleton
class HttpClient @Inject constructor(private val client: HttpClient) {
  private val logger = getLogger()

  private val counter = AtomicLong()

  fun toJava(): HttpClient = client

  val readTimeout = Duration.ofSeconds(5)

  fun get(
    uri: String,
    params: Map<String, Any>,
    headers: Map<String, String> = mapOf()
  ): Future<HttpResponse<String?>> {
    val request = makeGetRequest(uri, params, headers)
    return sendAsync(request)
  }

  fun post(uri: String, form: Map<String, Any>, headers: Map<String, String> = mapOf()): Future<HttpResponse<String?>> {
    val b = HttpRequest.newBuilder()
    b.uri(URI.create(uri))
      .timeout(readTimeout)
      .POST(HttpRequest.BodyPublishers.ofString(makeQueryString(form)))
    b.header("Content-Type", "application/x-www-form-urlencoded")
    headers.forEach { (k, v) -> b.header(k, v) }
    return sendAsync(b.build())
  }

  fun post(uri: String, json: String, headers: Map<String, String> = mapOf()): Future<HttpResponse<String?>> {
    val b = HttpRequest.newBuilder()
    b.uri(URI.create(uri))
      .timeout(readTimeout)
      .POST(HttpRequest.BodyPublishers.ofString(json))
    b.header("Content-Type", "application/json")
    headers.forEach { (k, v) -> b.header(k, v) }
    return sendAsync(b.build())
  }

  fun makeGetRequest(
    uri: String,
    params: Map<String, Any>,
    headers: Map<String, String>
  ): HttpRequest {
    val b = HttpRequest.newBuilder()
    b.uri(URI.create(uri + "?" + makeQueryString(params)))
      .timeout(Duration.ofSeconds(5))
      .GET()
    headers.forEach { (t, u) ->
      b.header(t, u)
    }
    return b.build()
  }

  fun makeQueryString(params: Map<String, Any>): String {
    return params.entries.mkString(
      '&',
      transform = { (k, v) ->
        URLEncoder.encode(k, Charsets.UTF_8) +
          "=" +
          URLEncoder.encode(v.toString(), Charsets.UTF_8)
      })
  }

  fun sendAsync(request: HttpRequest): Future<HttpResponse<String?>> {
    val requestNo = counter.incrementAndGet()
    logRequest(requestNo, request)
    return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
      .toFuture()
      .andThen { logResponse(requestNo, it) }
      .filter { it.isSuccess() }
  }

  private fun logRequest(requestNo: Long, request: HttpRequest) {
    request.bodyPublisher().filter { it.contentLength() > 0 }.ifPresentOrElse(
      {
        it.subscribe(object : Flow.Subscriber<ByteBuffer?> {
          override fun onComplete() {
          }

          override fun onSubscribe(subscription: Flow.Subscription) {
            subscription.request(1)
          }

          override fun onNext(item: ByteBuffer?) {
            item?.asReadOnlyBuffer()?.let { buffer ->
              val byteArray = ByteArray(buffer.remaining())
              buffer.get(byteArray)
              logger.info { "[$requestNo] sending http request: $request ${String(byteArray, Charsets.UTF_8)}" }
            }
          }

          override fun onError(throwable: Throwable) {
            logger.error(throwable) { "error occurred when subscribing http request: $request" }
            if (throwable.isFatal()) {
              throw throwable
            }
          }
        })
      },
      { logger.info { "[$requestNo] sending http request: $request" } }
    )
  }

  private fun logResponse(requestNo: Long, maybeResponse: Try<HttpResponse<String>>) {
    if (maybeResponse.isFailure) {
      var cause = maybeResponse.cause
      if (cause is CompletionException) {
        cause = cause.cause
      }
      when (cause) {
        is HttpConnectTimeoutException -> logger.error { "[$requestNo] http request connect timeout" }
        is HttpTimeoutException -> logger.error { "[$requestNo] http request timeout" }
        else -> logger.error(cause) { "[$requestNo] http request failure: ${cause.javaClass.simpleName}" }
      }
    } else {
      val response = maybeResponse.get()
      logger.info { "[$requestNo] http response: ${response.statusCode()} ${response.body()}" }
    }
  }
}

fun <T> HttpResponse<T?>.isSuccess(): Boolean = HttpStatusCode.isSuccess(statusCode())


@Singleton
class DefaultHttpClientProvider : Provider<HttpClient> {
  private val client = makeClient()

  private fun makeClient(): HttpClient {
    return HttpClient.newBuilder()
      .version(HttpClient.Version.HTTP_1_1)
      .followRedirects(HttpClient.Redirect.NORMAL)
      .connectTimeout(Duration.ofSeconds(2))
//      .authenticator(Authenticator.getDefault())
      .build()
  }

  override fun get(): HttpClient = client
}
