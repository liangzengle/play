package play.net.http

import org.junit.jupiter.api.Test
import java.time.Duration

internal class HttpClientTest {

  private val jhttpClient = JHttpClient
    .newBuilder()
    .version(java.net.http.HttpClient.Version.HTTP_1_1)
    .followRedirects(java.net.http.HttpClient.Redirect.NORMAL)
    .connectTimeout(Duration.ofSeconds(2))
//      .authenticator(Authenticator.getDefault())
    .build()

  private val httpClient = HttpClient("test", jhttpClient, Duration.ofSeconds(3))

  @Test
  fun get() {
    val result = httpClient.get("http://www.baidu.com", mapOf()).blockingGet()
    println(result.body())
  }

  @Test
  fun post() {
  }

  @Test
  fun testPost() {
  }
}
