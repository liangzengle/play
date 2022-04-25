package play.util.codec

import kotlinx.serialization.BinaryFormat
import kotlinx.serialization.serializer

class KSerializerCodecFactory(private val binaryFormat: BinaryFormat): CodecFactory {

  override fun <T: Any> newCodec(type: Class<T>): Codec<T> {
    return KSerializerCodec(binaryFormat, type.kotlin.serializer())
  }
}
