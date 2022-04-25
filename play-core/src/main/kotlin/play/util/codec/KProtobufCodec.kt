package play.util.codec

import kotlinx.serialization.KSerializer
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.serializer
import mu.KLogging
import play.util.ClassUtil
import play.util.EmptyByteArray
import play.util.unsafeCast
import kotlin.reflect.KClass

class KProtobufCodec<T : Any>(private val type: KClass<T>) : Codec<T> {
  private val serializer = type.serializer()

  override fun encode(value: T?): ByteArray {
    if (value == null) {
      return EmptyByteArray
    }
    return encodeToByteArray0(value, serializer)
  }

  override fun decode(value: ByteArray): T {
    return decodeFromByteArray(value, type, serializer)
  }

  companion object : KLogging() {

    @JvmStatic
    private fun <T : Any> encodeToByteArray0(value: T, serializer: KSerializer<T>): ByteArray {
      return when (value) {
        Unit -> EmptyByteArray
        is ByteArray -> value
        else -> ProtoBuf.encodeToByteArray(serializer, value)
      }
    }

    @JvmStatic
    fun <T : Any> encodeToByteArray(value: T?): ByteArray {
      if (value == null) {
        return EmptyByteArray
      }
      return encodeToByteArray0(value, value::class.serializer().unsafeCast())
    }

    @JvmStatic
    fun <T : Any> decodeFromByteArray(bytes: ByteArray, type: KClass<T>, serializer: KSerializer<T>): T {
      if (bytes.isEmpty()) {
        val primitiveType = ClassUtil.getPrimitiveType(type.java)
        if (primitiveType != null) {
          val defaultValue = ClassUtil.getPrimitiveDefaultValue(primitiveType)
          @Suppress("UNCHECKED_CAST")
          return defaultValue as T
        }
      }
      return try {
        ProtoBuf.decodeFromByteArray(serializer, bytes)
      } catch (e: Exception) {
        logger.error(e) { "decode failed: type=$type, bytes=${bytes.contentToString()}" }
        throw e
      }
    }
  }
}
