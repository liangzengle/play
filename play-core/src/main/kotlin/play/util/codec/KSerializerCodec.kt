package play.util.codec

import kotlinx.serialization.BinaryFormat
import kotlinx.serialization.KSerializer
import play.util.EmptyByteArray

class KSerializerCodec<T : Any>(private val binaryFormat: BinaryFormat, private val serializer: KSerializer<T>) :
  Codec<T> {
  override fun encode(value: T?): ByteArray {
    if (value == null) {
      return EmptyByteArray
    }
    return binaryFormat.encodeToByteArray(serializer, value)
  }

  override fun decode(value: ByteArray): T {
    return binaryFormat.decodeFromByteArray(serializer, value)
  }
}
