package play.httpclient

import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import kotlin.time.measureTime

/**
 *
 * @author LiangZengle
 */
class KtorHttpClientTest {

  private val httpClient = KtorHttpClient()

  @Test
  fun get() {
    val n = 10000
    val cost = measureTime {
      val cdl = CountDownLatch(n)
      for (i in 1..n) {
        httpClient.get("http://localhost:8080", mapOf()).whenComplete { t, e ->
          if (e != null) {
            e.printStackTrace()
          } else {
            println(t)
          }
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
}
