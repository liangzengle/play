package play.example.common.net.client

import io.netty.bootstrap.Bootstrap
import io.netty.channel.ChannelOption
import play.example.common.net.codec.RequestEncoder
import play.example.common.net.codec.ResponseDecoder
import play.example.common.net.message.WireRequestBody
import play.example.module.account.controller.AccountControllerInvoker
import play.example.module.account.message.LoginProto
import play.example.request.RequestProto
import play.mvc.Header
import play.mvc.MsgId
import play.mvc.Request
import play.net.netty.TcpClient
import play.net.netty.channelInitializer
import play.net.netty.createChannelFactory
import play.net.netty.createEventLoopGroup

/**
 *
 * @author LiangZengle
 */
object Client {
  private val eventLoop = createEventLoopGroup("client", 1)

  @JvmStatic
  fun main(args: Array<String>) {
    val client = TcpClient("test", "localhost", 8080, bootstrap())
    client.connect()
    val loginMsg = LoginProto("dev", 1, "", "someone")
    val msg = RequestProto(byteList = loginMsg.encodeByteString())
    client.write(
      Request(
        Header(MsgId(AccountControllerInvoker.login), 1),
        WireRequestBody(msg)
      )
    )
  }

  fun bootstrap(): Bootstrap {
    return Bootstrap()
      .group(eventLoop)
      .channelFactory(createChannelFactory())
      .option(ChannelOption.TCP_NODELAY, true)
      .option(ChannelOption.SO_SNDBUF, 1024 * 8)
      .option(ChannelOption.SO_RCVBUF, 1024 * 4)
      .channelInitializer {
        it.pipeline().addLast(RequestEncoder)
        it.pipeline().addLast(ResponseDecoder)
        it.pipeline().addLast(SimpleClientHandler)
      }
  }

}
