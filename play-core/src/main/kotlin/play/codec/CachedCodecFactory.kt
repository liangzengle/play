package play.codec

/**
 *
 *
 * @author LiangZengle
 */
abstract class CachedCodecFactory : CodecFactory {

  private val codecCache = object : ClassValue<Codec<*>>() {
    override fun computeValue(type: Class<*>): Codec<*> {
      return newCodec0(type)
    }
  }

  @Suppress("UNCHECKED_CAST")
  final override fun <T : Any> newCodec(type: Class<T>): Codec<T> {
    return codecCache.get(type) as Codec<T>
  }

  protected abstract fun <T : Any> newCodec0(type: Class<T>): Codec<T>
}
