package play.httpclient

import com.google.common.net.HttpHeaders
import com.google.common.net.MediaType
import io.micrometer.core.instrument.Metrics
import play.util.concurrent.Future.Companion.toPlay
import play.util.concurrent.PlayFuture
import play.util.logging.PlayLoggerFactory
import java.net.URI
import java.net.URLEncoder
import java.net.http.*
import java.nio.ByteBuffer
import java.time.Duration
import java.util.concurrent.CompletionException
import java.util.concurrent.Executors
import java.util.concurrent.Flow
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLongFieldUpdater

/**
 * Created by LiangZengle on 2020/2/20.
 */
class JdkHttpClient(
  private val name: String,
  private val client: HttpClient,
) : PlayHttpClient {

  companion object{
    private val requestNoUpdater = AtomicLongFieldUpdater.newUpdater(JdkHttpClient::class.java, "requestNo")
  }


  private val timer = Metrics.timer("httpclient", "name", name)

  private val logger = PlayLoggerFactory.getLogger(this.javaClass.name + '.' + name)

  @Volatile
  private var requestNo = 0L

  private fun nextRequestNo() = requestNoUpdater.incrementAndGet(this)

  fun toJava(): HttpClient = client

  override fun toString(): String {
    return "JdkHttpClient($name)"
  }

  override fun close() {
    client.close()
  }

  fun sendAsync(request: HttpRequest): PlayFuture<String> {
    return sendAsync(request, HttpResponse.BodyHandlers.ofString()).map { it.body() ?: "" }
  }

  fun <Body> sendAsync(
    request: HttpRequest,
    bodyHandler: HttpResponse.BodyHandler<Body>
  ): PlayFuture<HttpResponse<Body>> {
    val requestNo = nextRequestNo()
    logRequest(requestNo, request)
    val startTime = System.nanoTime()
    return client.sendAsync(request, bodyHandler)
      .toPlay()
      .andThen { v, e ->
        timer.record(System.nanoTime() - startTime, TimeUnit.NANOSECONDS)
        logResponse(requestNo, request, v, e)
      }
      .rejectIf({ !it.isSuccess() }, { UnsuccessfulStatusCodeException(it.uri().toString(), it.statusCode()) })
  }

  private fun logRequest(requestNo: Long, request: HttpRequest) {
    request.bodyPublisher().ifPresentOrElse(
      { publisher ->
        publisher.subscribe(object : Flow.Subscriber<ByteBuffer?> {
          override fun onSubscribe(subscription: Flow.Subscription) {
            subscription.request(1)
          }

          override fun onNext(item: ByteBuffer?) {
            if (logger.isInfoEnabled()) {
              val content = item?.asReadOnlyBuffer()?.let { buffer ->
                val byteArray = ByteArray(buffer.remaining())
                buffer.get(byteArray)
                String(byteArray, Charsets.UTF_8)
              } ?: ""
              logger.info { "[$requestNo] sending http request: $request $content" }
            }
          }

          override fun onError(throwable: Throwable) {
            logger.error(throwable) { "[$requestNo] observing http request error: $request" }
          }

          override fun onComplete() {
          }
        })

      },
      { logger.info { "[$requestNo] sending http request: $request" } }
    )
  }

  private fun logResponse(
    requestNo: Long,
    request: HttpRequest,
    response: HttpResponse<*>?,
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
    } else {
      checkNotNull(response)
      logger.info { "[$requestNo] http response: ${response.statusCode()} ${response.body()}" }
    }
  }

  override fun get(): Get {
    return GetImpl(this)
  }

  override fun post(): Post {
    return PostImpl(this)
  }

  class Builder(private val name: String) {
    private var clientBuilder = HttpClient.newBuilder()
      .connectTimeout(PlayHttpClient.DEFAULT_CONNECT_TIMEOUT)
      .executor(Executors.newVirtualThreadPerTaskExecutor())

    fun connectTimeout(timeout: Duration): Builder {
      clientBuilder.connectTimeout(timeout)
      return this
    }

    fun customize(clientCustomizer: HttpClient.Builder.() -> Unit): Builder {
      clientCustomizer(clientBuilder)
      return this;
    }

    fun build(): JdkHttpClient {
      return JdkHttpClient(name, clientBuilder.build())
    }
  }

  private class GetImpl(private val client: JdkHttpClient) : Get() {
    override fun sendAsync(): PlayFuture<String> {
      val uri = if (params.isEmpty()) {
        URI.create(this.uri)
      } else {
        val b = StringBuilder(64)
        b.append(this.uri).append('?')
        var first = true
        for (param in params) {
          if (!first) {
            b.append('&')
          } else {
            first = false
          }
          b.append(URLEncoder.encode(param.name, Charsets.UTF_8))
            .append('=')
            .append(URLEncoder.encode(param.value, Charsets.UTF_8))
        }
        URI.create(b.toString())
      }
      val request = HttpRequest.newBuilder().GET()
        .uri(uri)
        .timeout(timeout)
        .apply { headers.forEach { header(it.name, it.value) } }
        .build()
      return client.sendAsync(request)
    }
  }

  private class PostImpl(private val client: JdkHttpClient) : Post() {
    override fun sendAsync(): PlayFuture<String> {
      val requestBuilder = HttpRequest.newBuilder().uri(URI.create(this.uri)).timeout(timeout)
      if (form.isNotEmpty()) {
        val b = StringBuilder(64)
        for (param in form) {
          if (b.isNotEmpty()) {
            b.append('&')
          }
          b.append(URLEncoder.encode(param.name, Charsets.UTF_8))
            .append('=')
            .append(URLEncoder.encode(param.value, Charsets.UTF_8))
        }
        requestBuilder.POST(HttpRequest.BodyPublishers.ofString(b.toString()))
        requestBuilder.header(HttpHeaders.CONTENT_TYPE, MediaType.FORM_DATA.toString())
      }
      if (body != null) {
        requestBuilder.POST(HttpRequest.BodyPublishers.ofString(body))
        requestBuilder.header(HttpHeaders.CONTENT_TYPE, MediaType.JSON_UTF_8.toString())
      }
      headers.forEach { requestBuilder.header(it.name, it.value) }
      return client.sendAsync(requestBuilder.build())
    }
  }
}
