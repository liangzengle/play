package play.codec.protobuf

import com.google.protobuf.MessageLite
import play.codec.CodecFactory
import play.codec.MessageCodec
import kotlin.reflect.KClass

/**
 *
 *
 * @author LiangZengle
 */
class ProtobufMessageCodec : MessageCodec, CodecFactory by ProtobufCodecFactory() {

  override fun encode(msg: Any?): ByteArray {
    if (msg == null) {
      return ByteArray(0)
    }
    return (msg as MessageLite).toByteArray()
  }

  override fun <T : Any> decode(bytes: ByteArray, type: Class<T>): T {
    return newCodec(type).decode(bytes)
  }

  override fun <T : Any> decode(bytes: ByteArray, type: KClass<T>): T {
    return newCodec(type.java).decode(bytes)
  }
}
