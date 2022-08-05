package play.codec

/**
 *
 *
 * @author LiangZengle
 */
interface CodecFactory {
  fun <T : Any> newCodec(type: Class<T>): Codec<T>
}
