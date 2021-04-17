package play.example.client

import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import java.util.concurrent.TimeUnit
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import play.example.game.base.net.codec.PB
import play.example.game.module.account.controller.AccountControllerInvoker
import play.example.game.module.guild.GuildControllerInvoker
import play.example.game.module.guild.message.GuildInfo
import play.example.game.module.player.controller.PlayerControllerInvoker
import play.example.game.module.player.message.PlayerDTO
import play.mvc.*
import play.util.unsafeCast

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
      GuildControllerInvoker.create -> onGuildCreate(msg)
      PlayerControllerInvoker.ping -> pingPingResult(msg)
      PlayerControllerInvoker.StringMessage -> println("收到消息推送：${PB.decode<String>(msg.body!!.unsafeCast())}")
      AccountControllerInvoker.ping -> {
        val pong = PB.decode<String>(msg.body!!.unsafeCast())
        println("pong: $pong")
      }
      else -> println("receive: $msg")
    }
  }

  private fun onGuildCreate(response: Response) {
    if (response.statusCode != 0) {
      println("工会创建失败, 错误码：${response.statusCode}")
      return
    }
    val guild = ProtoBuf.decodeFromByteArray<GuildInfo>(response.body!!.unsafeCast())
    println("工会创建完成: $guild")
  }

  private fun pingPingResult(response: Response) {
    if (response.statusCode != 0) {
      println("ping失败, 错误码：${response.statusCode}")
      return
    }
    val msg = response.body
    val pong = PB.decode<String>(msg!!.unsafeCast())
    println("pong: $pong")
  }

  private fun onPlayerLogin(ctx: ChannelHandlerContext, msg: Response) {
    if (msg.statusCode != 0) {
      println("玩家登录失败：${msg.statusCode}")
      return
    }
    val playerInfo = PB.decode<PlayerDTO>(msg.body!!.unsafeCast())
    println("玩家登录成功：$playerInfo")
    ctx.executor().scheduleWithFixedDelay(
      {
        val pingMsg = "hello"
        ctx.writeAndFlush(
          Request(
            Header(MsgId(PlayerControllerInvoker.ping)),
            RequestBody(s1 = pingMsg)
          )
        )
        println("ping: $pingMsg")
      },
      0,
      2,
      TimeUnit.SECONDS
    )

    ctx.writeAndFlush(
      Request(
        Header(MsgId(GuildControllerInvoker.create)),
        RequestBody(s1 = "丐帮")
      )
    )

    ctx.writeAndFlush(
      Request(
        Header(MsgId(GuildControllerInvoker.create)),
        RequestBody(s1 = "少林")
      )
    )
  }

  private fun onPlayerCreate(ctx: ChannelHandlerContext, response: Response) {
    if (response.statusCode != 0) {
      println("创角失败, 错误码：${response.statusCode}")
    } else {
      println("创角成功, 请求角色登录")
      ctx.writeAndFlush(
        Request(
          Header(MsgId(PlayerControllerInvoker.login)),
          RequestBody()
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
    val hasPlayer = PB.decode<Boolean>(msg!!.unsafeCast())
    if (hasPlayer) {
      println("账号登录成功, 角色已存在, 请求角色登录")
      ctx.writeAndFlush(
        Request(
          Header(MsgId(PlayerControllerInvoker.login)),
          RequestBody()
        )
      )
    } else {
      println("账号登录成功, 角色不存在，请求创角")
      val request = RequestBody(s1 = "我是玩家名称")
      ctx.writeAndFlush(
        Request(
          Header(MsgId(PlayerControllerInvoker.create)),
          request
        )
      )
    }
  }
}
