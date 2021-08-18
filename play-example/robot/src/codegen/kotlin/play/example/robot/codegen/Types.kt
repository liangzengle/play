package play.example.robot.codegen

import com.squareup.kotlinpoet.ClassName

/**
 *
 * @author LiangZengle
 */
internal object Types {
  val RequestParams = ClassName.bestGuess("play.example.robot.net.RequestParams")

  val MessageCodec = ClassName.bestGuess("play.mvc.MessageCodec")

  val MsgId = ClassName.bestGuess("play.mvc.MsgId")

  val RequestBody = ClassName.bestGuess("play.mvc.RequestBody")

  val Response = ClassName.bestGuess("play.mvc.Response")

  val RequestBodyBuilder = ClassName.bestGuess("play.mvc.RequestBodyBuilder")

  val RobotPlayer = ClassName.bestGuess("play.example.robot.module.player.RobotPlayer")

  val ChannelHandlerContext = ClassName.bestGuess("io.netty.channel.ChannelHandlerContext")
}

