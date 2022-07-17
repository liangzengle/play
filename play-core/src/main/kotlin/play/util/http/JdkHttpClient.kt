package play.util.http

import org.reactivestreams.FlowAdapters
import play.httpclient.PlayHttpClient
import play.httpclient.UnsuccessfulStatusCodeException
import play.util.concurrent.Future.Companion.toPlay
import play.util.logging.Logger
import play.util.mkStringTo
import reactor.core.publisher.Flux
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpConnectTimeoutException
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.HttpTimeoutException
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.atomic.AtomicLong

typealias JHttpClient = java.net.http.HttpClient

/**
 * Created by LiangZengle on 2020/2/20.
 */
class JdkHttpClient constructor(
  private val client: JHttpClient,
  private val readTimeout: Duration = Duration.ofSeconds(5)
) : PlayHttpClient {

  companion object {
    private val log = Logger(PlayHttpClient.LOGGER)
  }

  init {
    require(readTimeout > Duration.ZERO) { "illegal readTimeout: $readTimeout" }
  }

  private val counter = AtomicLong()

  fun toJava(): JHttpClient = client

  override fun close() {
    // nothing to do
  }

  override fun toString(): String {
    return "JdkHttpClient"
  }

  fun copy(readTimeout: Duration): JdkHttpClient {
    return JdkHttpClient(client, readTimeout)
  }

  override fun get(
    url: String,
    params: Map<String, Any>,
    headers: Map<String, String>
  ): CompletableFuture<String> {
    val request = makeGetRequest(url, params, headers)
    return sendAsync(request)
  }

  override fun post(
    url: String,
    form: Map<String, Any>,
    headers: Map<String, String>
  ): CompletableFuture<String> {
    val b = HttpRequest.newBuilder()
    b.uri(URI.create(url))
      .timeout(readTimeout)
      .POST(HttpRequest.BodyPublishers.ofString(makeQueryString(form)))
    b.header("Content-Type", "application/x-www-form-urlencoded")
    headers.forEach { (k, v) -> b.header(k, v) }
    return sendAsync(b.build())
  }

  override fun post(
    url: String,
    data: String,
    headers: Map<String, String>
  ): CompletableFuture<String> {
    val b = HttpRequest.newBuilder()
    b.uri(URI.create(url))
      .timeout(readTimeout)
      .POST(HttpRequest.BodyPublishers.ofString(data))
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
    b.timeout(readTimeout).GET()
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

  fun sendAsync(request: HttpRequest): CompletableFuture<String> {
    val requestNo = counter.incrementAndGet()
    logRequest(requestNo, request)
    return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
      .toPlay()
      .andThen { v, e -> logResponse(requestNo, request, v, e) }
      .rejectIf({ !it.isSuccess() }, { UnsuccessfulStatusCodeException(it.uri().toString(), it.statusCode()) })
      .map { it.body() ?: "" }
      .toJava()
  }

  fun <Body> sendAsync(
    request: HttpRequest,
    bodyHandler: HttpResponse.BodyHandler<Body>
  ): CompletableFuture<HttpResponse<Body>> {
    val requestNo = counter.incrementAndGet()
    logRequest(requestNo, request)
    return client.sendAsync(request, bodyHandler)
      .toPlay()
      .andThen { v, e -> logResponse(requestNo, request, v, e) }
      .rejectIf({ !it.isSuccess() }, { UnsuccessfulStatusCodeException(it.uri().toString(), it.statusCode()) })
      .toJava()
  }

  private fun logRequest(requestNo: Long, request: HttpRequest) {
    request.bodyPublisher().ifPresentOrElse(
      { publisher ->
        Flux.from(FlowAdapters.toPublisher(publisher))
          .doOnNext {
            val content = it?.asReadOnlyBuffer()?.let { buffer ->
              val byteArray = ByteArray(buffer.remaining())
              buffer.get(byteArray)
              String(byteArray, Charsets.UTF_8)
            } ?: ""
            log.info { "[$requestNo] sending http request: $request $content" }
          }
          .doOnError {
            log.error(it) { "[$requestNo] observing http request error: $request" }
          }
      },
      { log.info { "[$requestNo] sending http request: $request" } }
    )
  }

  private fun logResponse(
    requestNo: Long,
    request: HttpRequest,
    response: HttpResponse<*>?,
    exception: Throwable?
  ) {
    if (response == null && exception == null) {
      log.error { "[$requestNo][$request] returns nothing." }
    } else if (exception != null) {
      var cause: Throwable = exception
      if (cause is CompletionException) {
        cause = cause.cause ?: cause
      }
      when (cause) {
        is HttpConnectTimeoutException -> log.error { "[$requestNo] http request connect timeout" }
        is HttpTimeoutException -> log.error { "[$requestNo] http request timeout" }
        else -> log.error(cause) { "[$requestNo] http request failure: ${cause.javaClass.simpleName}" }
      }
    } else {
      checkNotNull(response)
      log.info { "[$requestNo] http response: ${response.statusCode()} ${response.body()}" }
    }
  }
}
