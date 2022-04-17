package play.net.netty.http

import org.asynchttpclient.AsyncHttpClient
import org.asynchttpclient.AsyncHttpClientConfig
import org.asynchttpclient.BoundRequestBuilder
import org.asynchttpclient.Dsl
import play.net.http.HttpStatusCode
import play.util.concurrent.Future
import play.util.concurrent.Promise
import play.util.http.PlayHttpClient
import play.util.http.UnsuccessfulStatusCodeException
import java.util.concurrent.Executor

class NettyHttpClient(private val name: String, private val async: AsyncHttpClient, private val executor: Executor) :
  PlayHttpClient {

  constructor(name: String, config: AsyncHttpClientConfig, executor: Executor) : this(
    name,
    Dsl.asyncHttpClient(config),
    executor
  )

  constructor(name: String, connectTimeoutMillis: Int, readTimeoutMillis: Int, executor: Executor) : this(
    name,
    Dsl.config().setIoThreadsCount(1).setConnectTimeout(connectTimeoutMillis).setReadTimeout(readTimeoutMillis).build(),
    executor
  )

  constructor(name: String, executor: Executor) : this(name, 2000, 5000, executor)

  fun asyncHttpClient() = async

  override fun close() {
    async.close()
  }

  override fun toString(): String {
    return "NettyHttpClient($name)"
  }

  override fun get(url: String, params: Map<String, Any>, headers: Map<String, String>): Future<String> {
    val request = async.prepareGet(url)
    params.forEach { request.addQueryParam(it.key, it.value.toString()) }
    headers.forEach { request.addHeader(it.key, it.value) }
    return execute(request)
  }

  override fun post(url: String, form: Map<String, Any>, headers: Map<String, String>): Future<String> {
    val request = async.preparePost(url)
    form.forEach { request.addFormParam(it.key, it.value.toString()) }
    headers.forEach { request.addHeader(it.key, it.value) }
    return execute(request)
  }

  override fun post(url: String, data: String, headers: Map<String, String>): Future<String> {
    val request = async.preparePost(url)
    request.setBody(data)
    headers.forEach { request.addHeader(it.key, it.value) }
    return execute(request)
  }

  private fun execute(request: BoundRequestBuilder): Future<String> {
    val promise = Promise.make<String>()
    request.execute().toCompletableFuture().whenCompleteAsync(
      { response, exception ->
        if (exception != null) {
          promise.failure(exception)
        } else {
          if (HttpStatusCode.isSuccess(response.statusCode)) {
            promise.success(response.responseBody ?: "")
          } else {
            promise.failure(UnsuccessfulStatusCodeException(response.uri.toString(), response.statusCode))
          }
        }
      },
      executor
    )
    return promise.future
  }
}
