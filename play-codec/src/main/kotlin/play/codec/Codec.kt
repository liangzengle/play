package play.codec

interface Codec<T: Any> {
  fun encode(value: T?): ByteArray

  fun decode(bytes: ByteArray): T
}
