package play.example.robot.codegen

import com.squareup.kotlinpoet.*
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import kotlin.reflect.jvm.internal.impl.builtins.jvm.JavaToKotlinClassMap
import kotlin.reflect.jvm.internal.impl.name.FqName

/**
 *
 * @author LiangZengle
 */
internal object Types {
  val PB = ClassName.bestGuess("play.example.game.container.net.codec.PB")

  val RobotClient = ClassName.bestGuess("play.example.robot.net.RobotClient")

  val RequestParams = ClassName.bestGuess("play.example.robot.net.RequestParams")

  val Requester = ClassName.bestGuess("play.example.robot.net.Requester")

  val MsgId = ClassName.bestGuess("play.mvc.MsgId")

  val RequestBody = ClassName.bestGuess("play.mvc.RequestBody")

  val Response = ClassName.bestGuess("play.mvc.Response")

  val RequestBodyBuilder = ClassName.bestGuess("play.mvc.RequestBodyBuilder")

  val RobotPlayer = ClassName.bestGuess("play.example.robot.module.player.RobotPlayer")

  val ChannelHandlerContext = ClassName.bestGuess("io.netty.channel.ChannelHandlerContext")
}

class A{
  fun f(bytes: ByteArray) {

  }
}

fun main() {
  val m = A::class.java.getMethod("f", ByteArray::class.java)
  for (parameter in m.parameters) {
    test(parameter.parameterizedType)
    println(parameter.type.canonicalName)
    println(parameter.parameterizedType)
    val asTypeName = parameter.type.asTypeName()
    println(asTypeName)

    println(parameter.parameterizedType == ByteArray::class.java)
  }

  println(ByteArray::class.asClassName())
  println(ByteArray::class.java.name)
  println(ByteArray::class.java.canonicalName)
  val name = JavaToKotlinClassMap.INSTANCE.mapJavaToKotlin(FqName(ByteArray::class.java.canonicalName))?.asSingleFqName()
    ?.asString()?.let { ClassName.bestGuess(it) } ?: ByteArray::class.java.asClassName()
  println(name)
}

fun test(type: Type) {
 val t = when (type) {
    ByteArray::class.java -> BYTE_ARRAY
    IntArray::class.java -> INT_ARRAY
    LongArray::class.java -> LONG_ARRAY
    is Class<*> -> {
      JavaToKotlinClassMap.INSTANCE.mapJavaToKotlin(FqName(type.canonicalName))?.asSingleFqName()
        ?.asString()?.let { ClassName.bestGuess(it) } ?: type.asClassName()
    }
    is ParameterizedType -> {
      type.asTypeName()
    }
    else -> type.asTypeName()
  }
  println(t)
}

