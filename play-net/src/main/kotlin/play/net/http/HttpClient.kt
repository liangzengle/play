package play.net.http

import play.util.concurrent.Future
import play.util.concurrent.Future.Companion.toFuture
import play.util.exception.isFatal
import play.util.logging.getLogger
import play.util.mkStringTo
import java.net.URI
import java.net.URLEncoder
import java.net.http.*
import java.net.http.HttpClient
import java.nio.ByteBuffer
import java.time.Duration
import java.util.concurrent.CompletionException
import java.util.concurrent.Flow
import java.util.concurrent.atomic.AtomicLong

/**
 * Created by LiangZengle on 2020/2/20.
 */
class HttpClient constructor(
  private val client: HttpClient,
  private val readTimeout: Duration = Duration.ofSeconds(5)
) {
  companion object {
    private val logger = getLogger()
  }

  init {
    require(readTimeout > Duration.ZERO) { "readTimeout > Duration.ZERO" }
  }

  private val counter = AtomicLong()

  fun toJava(): HttpClient = client

  fun get(
    uri: String,
    params: Map<String, Any>,
    headers: Map<String, String> = mapOf()
  ): Future<HttpResponse<String>> {
    val request = makeGetRequest(uri, params, headers)
    return sendAsync(request)
  }

  fun post(
    uri: String,
    form: Map<String, Any>,
    headers: Map<String, String> = mapOf()
  ): Future<HttpResponse<String>> {
    val b = HttpRequest.newBuilder()
    b.uri(URI.create(uri))
      .timeout(readTimeout)
      .POST(HttpRequest.BodyPublishers.ofString(makeQueryString(form)))
    b.header("Content-Type", "application/x-www-form-urlencoded")
    headers.forEach { (k, v) -> b.header(k, v) }
    return sendAsync(b.build())
  }

  fun post(
    uri: String,
    json: String,
    headers: Map<String, String> = mapOf()
  ): Future<HttpResponse<String>> {
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
    val uriVal = if (params.isEmpty()) {
      URI.create(uri)
    } else {
      val uriBuilder = StringBuilder(32)
      uriBuilder.append(uri)
      if (uri.lastIndexOf('?') == -1) {
        uriBuilder.append('?')
      } else if (uri.last() != '?') {
        uriBuilder.append('&')
      }
      makeQueryStringTo(uriBuilder, params)
      URI.create(uriBuilder.toString())
    }
    val b = HttpRequest.newBuilder()
    b.uri(uriVal)
    b.timeout(Duration.ofSeconds(5)).GET()
    headers.forEach { (t, u) ->
      b.header(t, u)
    }
    return b.build()
  }

  fun makeQueryString(params: Map<String, Any>): String {
    val builder = StringBuilder()
    makeQueryStringTo(builder, params)
    return builder.toString()
  }

  fun makeQueryStringTo(builder: StringBuilder, params: Map<String, Any>) {
    if (params.isEmpty()) {
      return
    }
    params.entries.mkStringTo(
      builder,
      '&',
      transform = { (k, v) ->
        URLEncoder.encode(k, Charsets.UTF_8) +
          '=' +
          URLEncoder.encode(v.toString(), Charsets.UTF_8)
      }
    )
  }

  fun sendAsync(request: HttpRequest): Future<HttpResponse<String>> {
    val requestNo = counter.incrementAndGet()
    logRequest(requestNo, request)
    return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
      .toFuture()
      .andThen { v, e -> logResponse(requestNo, request, v, e) }
      .mapToFailure({ !it.isSuccess() }, { UnsuccessfulStatusCodeException(it.statusCode()) })
  }

  private fun logRequest(requestNo: Long, request: HttpRequest) {
    request.bodyPublisher().filter { it.contentLength() > 0 }.ifPresentOrElse(
      {
        it.subscribe(
          object : Flow.Subscriber<ByteBuffer?> {
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
          }
        )
      },
      { logger.info { "[$requestNo] sending http request: $request" } }
    )
  }

  private fun logResponse(
    requestNo: Long,
    request: HttpRequest,
    response: HttpResponse<String>?,
    exception: Throwable?
  ) {
    if (response == null && exception == null) {
      logger.error { "[$requestNo][$request] returns nothing." }
    } else if (exception != null) {
      var cause: Throwable = exception
      if (cause is CompletionException) {
        cause = cause.cause ?: cause
      }
      when (cause) {
        is HttpConnectTimeoutException -> logger.error { "[$requestNo] http request connect timeout" }
        is HttpTimeoutException -> logger.error { "[$requestNo] http request timeout" }
        else -> logger.error(cause) { "[$requestNo] http request failure: ${cause.javaClass.simpleName}" }
      }
    } else if (response != null) {
      logger.info { "[$requestNo] http response: ${response.statusCode()} ${response.body()}" }
    }
  }
}
