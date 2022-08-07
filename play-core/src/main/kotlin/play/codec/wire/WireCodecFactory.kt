package play.codec.wire

import com.squareup.wire.Message
import play.codec.CachedCodecFactory
import play.codec.Codec

/**
 *
 * @author LiangZengle
 */
class WireCodecFactory : CachedCodecFactory() {
  @Suppress("UNCHECKED_CAST")
  override fun <T : Any> newCodec0(type: Class<T>): Codec<T> {
    return WireCodec(type as Class<out Message<*, *>>) as Codec<T>
  }
}
