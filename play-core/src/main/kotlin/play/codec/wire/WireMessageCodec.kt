package play.codec.wire

import com.squareup.wire.Message
import okio.Buffer
import okio.BufferedSink
import okio.ByteString
import play.codec.CodecFactory
import play.codec.MessageCodec
import play.util.EmptyByteArray
import play.util.io.ByteBufferInputStream
import java.nio.ByteBuffer

/**
 *
 * @author LiangZengle
 */
class WireMessageCodec : MessageCodec, CodecFactory by WireCodecFactory() {

  fun encode(sink: BufferedSink, msg: Any) {
    when (msg) {
      is Message<*, *> -> msg.encode(sink)
      is DelegatingMessage -> msg.getMessage().encode(sink)
      else -> Wire.getBuiltinAdapter(msg.javaClass).encode(sink, msg)
    }
  }

  fun encodeToByteString(msg: Any?): ByteString {
    if (msg == null) {
      return ByteString.EMPTY
    }
    val sink = Buffer()
    encode(sink, msg)
    return sink.readByteString()
  }

  override fun encode(msg: Any?): ByteArray {
    if (msg == null) {
      return EmptyByteArray
    }
    val sink = Buffer()
    encode(sink, msg)
    return sink.readByteArray()
  }

  override fun <T : Any> decode(bytes: ByteArray, type: Class<T>): T {
    return if (Message::class.java.isAssignableFrom(type)) {
      newCodec(type).decode(bytes)
    } else {
      Wire.getBuiltinAdapter(type).decode(bytes)
    }
  }

  @Suppress("UNCHECKED_CAST")
  override fun <T : Any> decode(buffer: ByteBuffer, type: Class<T>): T {
    return if (Message::class.java.isAssignableFrom(type)) {
      (newCodec(type) as WireCodec<*>).decode(buffer) as T
    } else {
      Wire.getBuiltinAdapter(type).decode(ByteBufferInputStream(buffer))
    }
  }
}
