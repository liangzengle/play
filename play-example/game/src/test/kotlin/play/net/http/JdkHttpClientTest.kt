package play.net.http

import org.junit.jupiter.api.Test
import play.util.http.JHttpClient
import play.util.http.JdkHttpClient
import java.time.Duration
import java.util.concurrent.CountDownLatch
import kotlin.time.measureTime

internal class JdkHttpClientTest {

  private val jhttpClient = JHttpClient.newBuilder().version(java.net.http.HttpClient.Version.HTTP_1_1)
    .followRedirects(java.net.http.HttpClient.Redirect.NORMAL).connectTimeout(Duration.ofSeconds(2))
//      .authenticator(Authenticator.getDefault())
    .build()

  private val httpClient = JdkHttpClient("test", jhttpClient, Duration.ofSeconds(3))

  @Test
  fun get() {
    val n = 10000
    val cost = measureTime {
      val cdl = CountDownLatch(n)
      for (i in 1..n) {
        httpClient.get("http://localhost:8080", mapOf()).onComplete {
          cdl.countDown()
        }
      }
      cdl.await()
    }
    println("耗时: ${cost.inWholeMilliseconds}")
    httpClient
  }

  @Test
  fun post() {
  }

  @Test
  fun testPost() {
  }
}
