package play.codec

import play.codec.protobuf.ProtobufMessageCodec
import java.nio.ByteBuffer
import java.util.ServiceLoader
import kotlin.reflect.KClass

/**
 *
 *
 * @author LiangZengle
 */
interface MessageCodec : CodecFactory {

  fun encode(msg: Any?): ByteArray

  fun <T : Any> decode(bytes: ByteArray, type: Class<T>): T

  fun <T : Any> decode(bytes: ByteArray, type: KClass<T>): T {
    return decode(bytes, type.java)
  }

  companion object {
    private val Default: MessageCodec =
      ServiceLoader.load(MessageCodec::class.java).findFirst().orElseGet { ProtobufMessageCodec() }

    fun encode(msg: Any?): ByteArray = Default.encode(msg)
    fun <T : Any> decode(bytes: ByteArray, type: Class<T>): T = Default.decode(bytes, type)
    fun <T : Any> decode(bytes: ByteArray, type: KClass<T>): T = Default.decode(bytes, type)
    inline fun <reified T : Any> decode(bytes: ByteArray): T = decode(bytes, T::class)

    fun <T : Any> newCodec(type: KClass<T>): Codec<T> = Default.newCodec(type.java)
    fun <T : Any> newCodec(type: Class<T>): Codec<T> = Default.newCodec(type)
  }
}
