package play.codec.kprotobuf

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.serializer
import play.codec.Codec
import play.util.ClassUtil
import play.util.EmptyByteArray
import play.util.primitive.PrimitiveDefaults

/**
 *
 *
 * @author LiangZengle
 */
@OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
class KProtobufCodec<T : Any>(type: Class<T>) : Codec<T> {
  private val serializer = type.kotlin.serializer()
  private val primitiveType = ClassUtil.getPrimitiveType(type)

  override fun encode(value: T?): ByteArray {
    if (value == null) {
      return EmptyByteArray
    }
    return ProtoBuf.encodeToByteArray(serializer, value)
  }

  override fun decode(bytes: ByteArray): T {
    if (bytes.isEmpty() && primitiveType != null) {
      return PrimitiveDefaults.get(primitiveType)
    }
    return ProtoBuf.decodeFromByteArray(serializer, bytes)
  }
}
