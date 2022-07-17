package play.httpclient

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture

interface PlayHttpClient : AutoCloseable {

  companion object {
    @JvmStatic
    val LOGGER = LoggerFactory.getLogger(PlayHttpClient::class.java)
  }

  val logger: Logger get() = LOGGER

  fun get(url: String): CompletableFuture<String> {
    return get(url, mapOf())
  }

  fun get(url: String, params: Map<String, Any>): CompletableFuture<String> {
    return get(url, mapOf(), mapOf())
  }

  fun get(url: String, params: Map<String, Any>, headers: Map<String, String>): CompletableFuture<String>

  fun post(url: String): CompletableFuture<String> {
    return post(url, mapOf())
  }

  fun post(url: String, form: Map<String, Any>): CompletableFuture<String> {
    return post(url, mapOf(), mapOf())
  }

  fun post(url: String, form: Map<String, Any>, headers: Map<String, String>): CompletableFuture<String>

  fun post(url: String, data: String): CompletableFuture<String> {
    return post(url, data, mapOf())
  }

  fun post(url: String, data: String, headers: Map<String, String>): CompletableFuture<String>
}
