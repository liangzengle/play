package play.mvc

import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.serializer
import play.Log
import play.util.codec.Codec
import play.util.codec.CodecFactory
import play.util.codec.KProtobufCodec
import play.util.codec.KProtobufCodecFactory
import play.util.reflect.Reflect
import kotlin.reflect.KClass

interface MessageCodec : CodecFactory {
  fun encode(msg: Any?): ByteArray
  fun <T : Any> decode(bytes: ByteArray, type: Class<T>): T
  fun <T : Any> decode(bytes: ByteArray, type: KClass<T>): T

  companion object Default {
    private val default: MessageCodec =
      System.getProperty("play.mvc.message.codec")?.let { Reflect.getKotlinObjectOrNewInstance(it) } ?: ProtobufCodec

    init {
      Log.debug { "MessageCodec: ${default.javaClass.simpleName}" }
    }

    fun encode(msg: Any?): ByteArray = default.encode(msg)
    fun <T : Any> decode(bytes: ByteArray, type: Class<T>): T = default.decode(bytes, type)
    fun <T : Any> decode(bytes: ByteArray, type: KClass<T>): T = default.decode(bytes, type)
    inline fun <reified T : Any> decode(bytes: ByteArray): T = decode(bytes, T::class)

    fun <T : Any> newCodec(type: KClass<T>): Codec<T> = default.newCodec(type.java)
    fun <T : Any> newCodec(type: Class<T>): Codec<T> = default.newCodec(type)
  }
}

class MessageCodecAsCodec<T : Any>(private val messageCodec: MessageCodec, private val type: Class<T>) : Codec<T> {
  override fun encode(value: T?): ByteArray {
    return messageCodec.encode(value)
  }

  override fun decode(value: ByteArray): T {
    return messageCodec.decode(value, type)
  }
}

object ProtobufCodec : MessageCodec, CodecFactory by KProtobufCodecFactory {
  override fun encode(msg: Any?): ByteArray {
    return KProtobufCodec.encodeToByteArray(msg)
  }

  override fun <T : Any> decode(bytes: ByteArray, type: Class<T>): T {
    return decode(bytes, type.kotlin)
  }

  override fun <T : Any> decode(bytes: ByteArray, type: KClass<T>): T {
    return KProtobufCodec.decodeFromByteArray(bytes, type, type.serializer())
  }

  inline fun <reified T : Any> decode(bytes: ByteArray): T = ProtoBuf.decodeFromByteArray(bytes)
}
