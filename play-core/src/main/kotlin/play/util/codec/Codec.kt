package play.util.codec

interface Codec<T: Any> {
  fun encode(value: T?): ByteArray

  fun decode(value: ByteArray): T
}
