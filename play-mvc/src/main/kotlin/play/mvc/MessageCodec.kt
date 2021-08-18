package play.mvc

import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.serializer
import play.util.ClassUtil
import play.util.EmptyByteArray
import play.util.logging.getLogger
import play.util.unsafeCast
import kotlin.reflect.KClass

interface MessageCodec {
  fun encode(msg: Any?): ByteArray
  fun <T : Any> decode(bytes: ByteArray, type: Class<T>): T
  fun <T : Any> decode(bytes: ByteArray, type: KClass<T>): T

  companion object {
    var DEFAULT: MessageCodec = ProtobufCodec
      private set

    fun setDefault(messageCodec: MessageCodec) {
      DEFAULT = messageCodec
    }

    fun encode(msg: Any?): ByteArray = DEFAULT.encode(msg)
    fun <T : Any> decode(bytes: ByteArray, type: Class<T>): T = DEFAULT.decode(bytes, type)
    fun <T : Any> decode(bytes: ByteArray, type: KClass<T>): T = DEFAULT.decode(bytes, type)
    inline fun <reified T : Any> decode(bytes: ByteArray): T = decode(bytes, T::class)
  }
}

object ProtobufCodec : MessageCodec {
  private val logger = getLogger()

  override fun encode(msg: Any?): ByteArray {
    return when (msg) {
      null, Unit -> EmptyByteArray
      is ByteArray -> msg
      else -> ProtoBuf.encodeToByteArray(msg::class.serializer().unsafeCast(), msg)
    }
  }

  override fun <T : Any> decode(bytes: ByteArray, type: Class<T>): T {
    return decode(bytes, type.kotlin)
  }

  override fun <T : Any> decode(bytes: ByteArray, type: KClass<T>): T {
    if (bytes.isEmpty()) {
      val primitiveType = ClassUtil.getPrimitiveType(type.java)
      if (primitiveType != null) {
        val defaultValue = ClassUtil.getPrimitiveDefaultValue(primitiveType)
        @Suppress("UNCHECKED_CAST")
        return defaultValue as T
      }
    }
    return try {
      ProtoBuf.decodeFromByteArray(type.serializer(), bytes)
    } catch (e: Exception) {
      logger.error { "decode failed: type=$type, bytes=${bytes.contentToString()}" }
      throw e
    }
  }

  inline fun <reified T : Any> decode(bytes: ByteArray): T = ProtoBuf.decodeFromByteArray(bytes)
}
