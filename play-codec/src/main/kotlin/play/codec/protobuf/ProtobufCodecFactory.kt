package play.codec.protobuf

import com.google.protobuf.MessageLite
import play.codec.CachedCodecFactory
import play.codec.Codec

/**
 *
 *
 * @author LiangZengle
 */
class ProtobufCodecFactory : CachedCodecFactory() {
  @Suppress("UNCHECKED_CAST")
  override fun <T : Any> newCodec0(type: Class<T>): Codec<T> {
    return ProtobufCodec(type as Class<out MessageLite>) as Codec<T>
  }
}
