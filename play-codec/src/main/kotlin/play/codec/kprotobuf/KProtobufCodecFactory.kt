package play.codec.kprotobuf

import play.codec.CachedCodecFactory
import play.codec.Codec

/**
 *
 *
 * @author LiangZengle
 */
class KProtobufCodecFactory : CachedCodecFactory() {
  override fun <T : Any> newCodec0(type: Class<T>): Codec<T> {
    return KProtobufCodec(type)
  }
}
