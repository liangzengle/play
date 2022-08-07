package play.httpclient

import java.util.concurrent.CompletableFuture

/**
 *
 * @author LiangZengle
 */
class LoggingOnErrorHttpClient(private val delegate: PlayHttpClient) : PlayHttpClient {
  override fun get(url: String, params: Map<String, Any>, headers: Map<String, String>): CompletableFuture<String> {
    return delegate.get(url, params, headers)
      .whenComplete { _, exception ->
        if (exception != null) {
          delegate.logger.error("http request failed: $url $params", exception)
        }
      }
  }

  override fun post(url: String, form: Map<String, Any>, headers: Map<String, String>): CompletableFuture<String> {
    return delegate.post(url, form, headers)
      .whenComplete { _, exception ->
        if (exception != null) {
          delegate.logger.error("http request failed: $url $form", exception)
        }
      }
  }

  override fun post(url: String, data: String, headers: Map<String, String>): CompletableFuture<String> {
    return delegate.post(url, data, headers)
      .whenComplete { _, exception ->
        if (exception != null) {
          delegate.logger.error("http request failed: $url $data", exception)
        }
      }
  }

  override fun close() {
    delegate.close()
  }
}
