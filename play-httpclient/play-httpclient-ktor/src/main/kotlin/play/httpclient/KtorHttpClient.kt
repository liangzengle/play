package play.httpclient

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.future.future
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.ForkJoinPool

/**
 *
 * @author LiangZengle
 */
@Suppress("OPT_IN_USAGE")
class KtorHttpClient(private val ktor: HttpClient, private val workerPool: Executor) : PlayHttpClient {

  constructor() : this(
    HttpClient(CIO) {
      expectSuccess = true
      engine {
        threadsCount = 1
      }
      install(HttpTimeout) {
        requestTimeoutMillis = 5000
      }
    },
    ForkJoinPool.commonPool()
  )

  override fun get(url: String, params: Map<String, Any>, headers: Map<String, String>): CompletableFuture<String> {
    return handover(
      GlobalScope.future {
        val response = ktor.get(url) {
          headers {
            headers.forEach(::append)
          }
          url {
            params.forEach { parameters.append(it.key, it.value.toString()) }
          }
        }
        if (!response.status.isSuccess()) {
          throw UnsuccessfulStatusCodeException(url, response.status.value)
        }
        response.bodyAsText()
      }
    )
  }

  override fun post(url: String, form: Map<String, Any>, headers: Map<String, String>): CompletableFuture<String> {
    return handover(
      GlobalScope.future {
        val response = ktor.post(url) {
          headers {
            headers.forEach(::append)
          }
          formData {
            form.forEach { append(it.key, it.value.toString()) }
          }
        }
        if (!response.status.isSuccess()) {
          throw UnsuccessfulStatusCodeException(url, response.status.value)
        }
        response.bodyAsText()
      }
    )
  }

  override fun post(url: String, data: String, headers: Map<String, String>): CompletableFuture<String> {
    return handover(
      GlobalScope.future {
        val response = ktor.post(url) {
          headers {
            headers.forEach(::append)
          }
          setBody(data)
        }
        if (!response.status.isSuccess()) {
          throw UnsuccessfulStatusCodeException(url, response.status.value)
        }
        response.bodyAsText()
      }
    )
  }

  override fun close() {
    ktor.close()
  }

  /**
   * let [workerPool] take it from here
   */
  private fun <T> handover(future: CompletableFuture<T>): CompletableFuture<T> {
    val f = CompletableFuture<T>()
    future.whenCompleteAsync({ t, u ->
      if (u != null) {
        f.completeExceptionally(u)
      } else {
        f.complete(t)
      }
    }, workerPool)
    return f
  }
}
