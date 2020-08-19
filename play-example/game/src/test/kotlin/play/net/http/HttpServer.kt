package play.net.http

import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpServerCodec
import io.netty.handler.codec.http.HttpServerKeepAliveHandler
import mu.KLogging
import org.slf4j.Logger
import play.net.netty.NettyServerBuilder
import play.net.netty.http.NettyHttpServerHandler
import play.util.logging.NOOPLogger

internal object HttpServer : KLogging() {
  private val action = object : Action(RoutePath("/", emptyList()), emptyList()) {
    override fun invoke(request: AbstractHttpRequest): HttpResult {
      return HttpResult.Strict(200, HttpEntity.Strict("hello"))
    }
  }
  private val actionManager: HttpActionManager = object : HttpActionManager() {}.apply {
    register(listOf(action))
  }
  private val handler = object : NettyHttpServerHandler(actionManager) {
    override fun channelActive(ctx: ChannelHandlerContext) {
      super.channelActive(ctx)
      println("new channel: ${ctx.channel().id()}")
    }

    override val filters: List<HttpRequestFilter> = emptyList()
    override val logger: Logger = NOOPLogger
  }
  private val httpServer =
    NettyServerBuilder().host("localhost").port(8080).childHandler {
      it.pipeline()
        .addLast(HttpServerCodec())
        .addLast(HttpServerKeepAliveHandler())
        .addLast(HttpObjectAggregator(65535, true))
        .addLast(handler)
    }
      .build("http-server")

  @JvmStatic
  fun main(args: Array<String>) {
    httpServer.start()
  }
}
