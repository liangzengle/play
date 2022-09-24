package play.codec

import play.util.LambdaClassValue

/**
 *
 *
 * @author LiangZengle
 */
abstract class CachedCodecFactory : CodecFactory {

  private val codecCache = LambdaClassValue { newCodec0(it) }

  @Suppress("UNCHECKED_CAST")
  final override fun <T : Any> newCodec(type: Class<T>): Codec<T> {
    return codecCache.get(type) as Codec<T>
  }

  protected abstract fun <T : Any> newCodec0(type: Class<T>): Codec<T>
}
