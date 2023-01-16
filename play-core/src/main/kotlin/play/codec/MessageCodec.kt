package play.codec

import play.codec.kprotobuf.KProtobufMessageCodec
import play.util.ServiceLoader2
import java.nio.ByteBuffer
import kotlin.reflect.KClass

/**
 *
 *
 * @author LiangZengle
 */
interface MessageCodec : CodecFactory {

  fun encode(msg: Any?): ByteArray

  fun <T : Any> decode(bytes: ByteArray, type: Class<T>): T

  fun <T : Any> decode(buffer: ByteBuffer, type: Class<T>): T

  fun <T : Any> decode(bytes: ByteArray, type: KClass<T>): T {
    return decode(bytes, type.java)
  }

  companion object {
    val Default: MessageCodec = ServiceLoader2.loadOrDefault(MessageCodec::class.java) { KProtobufMessageCodec() }

    fun encode(msg: Any?): ByteArray = Default.encode(msg)
    fun <T : Any> decode(buffer: ByteBuffer, type: Class<T>): T = Default.decode(buffer, type)
    fun <T : Any> decode(bytes: ByteArray, type: Class<T>): T = Default.decode(bytes, type)
    fun <T : Any> decode(bytes: ByteArray, type: KClass<T>): T = Default.decode(bytes, type)
    inline fun <reified T : Any> decode(bytes: ByteArray): T = decode(bytes, T::class)

    fun <T : Any> newCodec(type: KClass<T>): Codec<T> = Default.newCodec(type.java)
    fun <T : Any> newCodec(type: Class<T>): Codec<T> = Default.newCodec(type)
  }
}
