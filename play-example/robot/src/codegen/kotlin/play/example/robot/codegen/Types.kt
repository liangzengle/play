package play.example.robot.codegen

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.MemberName

/**
 *
 * @author LiangZengle
 */
internal object Types {
  val RequestParams = ClassName.bestGuess("play.example.robot.net.RequestParams")

  val MessageCodec = ClassName.bestGuess("play.codec.MessageCodec")

  val MsgId = ClassName.bestGuess("play.mvc.MsgId")

  val RequestBody = ClassName.bestGuess("play.mvc.RequestBody")

  val Response = ClassName.bestGuess("play.mvc.Response")

  val RequestBodyFactory = ClassName.bestGuess("play.mvc.RequestBodyFactory")

  val RobotPlayer = ClassName.bestGuess("play.example.robot.module.player.RobotPlayer")

  val ChannelHandlerContext = ClassName.bestGuess("io.netty.channel.ChannelHandlerContext")

  val EmptyByteArray = MemberName("play.util", "EmptyByteArray")
}

