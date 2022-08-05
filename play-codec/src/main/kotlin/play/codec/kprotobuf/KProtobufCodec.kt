package play.codec.kprotobuf

import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.serializer
import play.codec.Codec
import play.util.PrimitiveTypes

/**
 *
 *
 * @author LiangZengle
 */
class KProtobufCodec<T : Any>(type: Class<T>) : Codec<T> {
  private val serializer = type.kotlin.serializer()
  private val primitiveType = PrimitiveTypes.getPrimitiveType(type)

  override fun encode(value: T?): ByteArray {
    if (value == null) {
      return ByteArray(0)
    }
    return ProtoBuf.encodeToByteArray(serializer, value)
  }

  override fun decode(bytes: ByteArray): T {
    if (bytes.isEmpty() && primitiveType != null) {
      return PrimitiveTypes.getPrimitiveDefaultValue(primitiveType) as T
    }
    return ProtoBuf.decodeFromByteArray(serializer, bytes)
  }
}
