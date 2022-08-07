package play.codec.kprotobuf

import play.codec.CodecFactory
import play.codec.MessageCodec
import play.util.EmptyByteArray
import java.nio.ByteBuffer
import kotlin.reflect.KClass

/**
 *
 *
 * @author LiangZengle
 */
class KProtobufMessageCodec : MessageCodec, CodecFactory by KProtobufCodecFactory() {

  override fun encode(msg: Any?): ByteArray {
    if (msg == null) {
      return EmptyByteArray
    }
    return newCodec(msg.javaClass).encode(msg)
  }

  override fun <T : Any> decode(buffer: ByteBuffer, type: Class<T>): T {
    val bytes = if (buffer.hasArray()) buffer.array() else {
      val dst = ByteArray(buffer.remaining())
      buffer.get(dst)
      dst
    }
    return newCodec(type).decode(bytes)
  }

  override fun <T : Any> decode(bytes: ByteArray, type: Class<T>): T {
    return newCodec(type).decode(bytes)
  }

  override fun <T : Any> decode(bytes: ByteArray, type: KClass<T>): T {
    return newCodec(type.java).decode(bytes)
  }
}
