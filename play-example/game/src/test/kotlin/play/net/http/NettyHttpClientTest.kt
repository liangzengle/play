package play.net.http

import org.junit.jupiter.api.Test
import play.net.netty.http.NettyHttpClient
import play.util.concurrent.CommonPool
import java.util.concurrent.CountDownLatch
import kotlin.time.measureTime

internal class NettyHttpClientTest {

  private val httpClient = NettyHttpClient("test", CommonPool)

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
