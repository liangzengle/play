package play.net.http

import com.sun.net.httpserver.HttpServer
import play.util.rd
import java.net.InetSocketAddress
import java.util.concurrent.Executors

/**
 *
 * @author LiangZengle
 */
fun main() {
  val httpServer = HttpServer.create(InetSocketAddress("localhost", 8088), 0)
  httpServer.executor = Executors.newFixedThreadPool(16)
  httpServer.createContext("/") {
    if (rd.nextBoolean()) {
      Thread.sleep(1000)
    }
    val content = "hello"
    it.sendResponseHeaders(200, content.length.toLong())
    it.responseBody.write(content.toByteArray())
    it.close()
  }
  httpServer.start()
  Thread.sleep(Int.MAX_VALUE.toLong())
}
