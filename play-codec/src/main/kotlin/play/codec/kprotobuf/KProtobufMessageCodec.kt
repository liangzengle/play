package play.codec.kprotobuf

import play.codec.CodecFactory
import play.codec.MessageCodec
import kotlin.reflect.KClass

/**
 *
 *
 * @author LiangZengle
 */
class KProtobufMessageCodec : MessageCodec, CodecFactory by KProtobufCodecFactory() {

  override fun encode(msg: Any?): ByteArray {
    if (msg == null) {
      return ByteArray(0)
    }
    return newCodec(msg.javaClass).encode(msg)
  }

  override fun <T : Any> decode(bytes: ByteArray, type: Class<T>): T {
    return newCodec(type).decode(bytes)
  }

  override fun <T : Any> decode(bytes: ByteArray, type: KClass<T>): T {
    return newCodec(type.java).decode(bytes)
  }
}
