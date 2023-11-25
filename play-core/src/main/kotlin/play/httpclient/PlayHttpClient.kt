package play.httpclient

import java.time.Duration

interface PlayHttpClient : AutoCloseable {

  companion object {
    @JvmStatic
    val DEFAULT_CONNECT_TIMEOUT: Duration = Duration.ofSeconds(2)

    @JvmStatic
    val DEFAULT_READ_TIMEOUT: Duration = Duration.ofSeconds(5)
  }

  fun get(): Get

  fun post(): Post
}
