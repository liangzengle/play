package play.example.common.net.client

import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import play.example.common.net.message.WireRequestBody
import play.example.module.account.controller.AccountControllerInvoker
import play.example.module.account.message.PongProto
import play.example.module.common.message.BoolValue
import play.example.module.common.message.StringValue
import play.example.module.player.controller.PlayerControllerInvoker
import play.example.module.player.message.PlayerProto
import play.example.request.RequestProto
import play.mvc.*
import java.util.concurrent.TimeUnit

/**
 *
 * @author LiangZengle
 */
@ChannelHandler.Sharable
object SimpleClientHandler : SimpleChannelInboundHandler<Response>() {
  override fun channelRead0(ctx: ChannelHandlerContext, msg: Response) {
    when (msg.header.msgId.toInt()) {
      AccountControllerInvoker.login -> onLogin(ctx, msg)
      PlayerControllerInvoker.create -> onPlayerCreate(ctx, msg)
      PlayerControllerInvoker.login -> onPlayerLogin(ctx, msg)
      PlayerControllerInvoker.ping -> pingPingResult(msg)
      AccountControllerInvoker.ping
      -> {
        val pong = PongProto.ADAPTER.decode(msg.body.toByteArray())
        println("pong: ${pong.msg}")
      }
      else -> println("receive: $msg")
    }
  }

  private fun pingPingResult(response: Response) {
    if (response.statusCode != 0) {
      println("ping失败, 错误码：${response.statusCode}")
      return
    }
    val msg = response.body
    if (msg !is ByteArrayMessage) {
      return
    }
    val pong = StringValue.ADAPTER.decode(msg.toByteArray()).value
    println("pong: $pong")
  }

  private fun onPlayerLogin(ctx: ChannelHandlerContext, msg: Response) {
    if (msg.statusCode != 0) {
      println("玩家登录失败：${msg.statusCode}")
      return
    }
    println("玩家登录成功：${PlayerProto.ADAPTER.decode(msg.body.toByteArray())}")
    ctx.executor().scheduleWithFixedDelay({
      val pingMsg = "hello"
      ctx.writeAndFlush(
        Request(
          Header(MsgId(PlayerControllerInvoker.ping)),
          WireRequestBody(RequestProto(s1 = pingMsg))
        )
      )
      println("ping: $pingMsg")
    }, 0, 2, TimeUnit.SECONDS)
  }

  private fun onPlayerCreate(ctx: ChannelHandlerContext, response: Response) {
    if (response.statusCode != 0) {
      println("创角失败, 错误码：${response.statusCode}")
    } else {
      println("创角成功, 请求角色登录")
      ctx.writeAndFlush(
        Request(
          Header(MsgId(PlayerControllerInvoker.login)),
          WireRequestBody(RequestProto())
        )
      )
    }
  }

  private fun onLogin(ctx: ChannelHandlerContext, response: Response) {
    if (response.statusCode != 0) {
      println("账号登录失败, 错误码：${response.statusCode}")
      return
    }
    val msg = response.body
    if (msg !is ByteArrayMessage) {
      return
    }
    val hasPlayer = BoolValue.ADAPTER.decode(msg.toByteArray()).value
    if (hasPlayer) {
      println("账号登录成功, 角色已存在, 请求角色登录")
      ctx.writeAndFlush(
        Request(
          Header(MsgId(PlayerControllerInvoker.login)),
          WireRequestBody(RequestProto())
        )
      )
    } else {
      println("账号登录成功, 角色不存在，请求创角")
      val request = RequestProto(s1 = "我是玩家名称")
      ctx.writeAndFlush(
        Request(
          Header(MsgId(PlayerControllerInvoker.create)),
          WireRequestBody(request)
        )
      )
    }
  }
}
