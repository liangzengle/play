package play.httpclient

import org.asynchttpclient.AsyncHttpClient
import org.asynchttpclient.AsyncHttpClientConfig
import org.asynchttpclient.BoundRequestBuilder
import org.asynchttpclient.Dsl
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

class NettyHttpClient(private val async: AsyncHttpClient, private val executor: Executor) :
  PlayHttpClient {

  constructor(config: AsyncHttpClientConfig, executor: Executor) : this(
    Dsl.asyncHttpClient(config),
    executor
  )

  constructor(requestTimeoutMillis: Int, executor: Executor) : this(
    Dsl.config().setIoThreadsCount(1).setRequestTimeout(requestTimeoutMillis).build(),
    executor
  )

  constructor(executor: Executor) : this(5000, executor)

  fun asyncHttpClient() = async

  override fun close() {
    async.close()
  }

  override fun toString(): String {
    return "NettyHttpClient"
  }

  override fun get(url: String, params: Map<String, Any>, headers: Map<String, String>): CompletableFuture<String> {
    val request = async.prepareGet(url)
    params.forEach { request.addQueryParam(it.key, it.value.toString()) }
    headers.forEach { request.addHeader(it.key, it.value) }
    return execute(request)
  }

  override fun post(url: String, form: Map<String, Any>, headers: Map<String, String>): CompletableFuture<String> {
    val request = async.preparePost(url)
    form.forEach { request.addFormParam(it.key, it.value.toString()) }
    headers.forEach { request.addHeader(it.key, it.value) }
    return execute(request)
  }

  override fun post(url: String, data: String, headers: Map<String, String>): CompletableFuture<String> {
    val request = async.preparePost(url)
    request.setBody(data)
    headers.forEach { request.addHeader(it.key, it.value) }
    return execute(request)
  }

  private fun execute(request: BoundRequestBuilder): CompletableFuture<String> {
    val promise = CompletableFuture<String>()
    request.execute().toCompletableFuture().whenCompleteAsync(
      { response, exception ->
        if (exception != null) {
          promise.completeExceptionally(exception)
        } else {
          if (response.statusCode in 200..299) {
            promise.complete(response.responseBody ?: "")
          } else {
            promise.completeExceptionally(UnsuccessfulStatusCodeException(response.uri.toString(), response.statusCode))
          }
        }
      },
      executor
    )
    return promise
  }
}
