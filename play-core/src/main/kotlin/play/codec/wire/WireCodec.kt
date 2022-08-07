package play.codec.wire

import com.squareup.wire.Message
import com.squareup.wire.ProtoAdapter
import play.codec.Codec
import play.util.EmptyByteArray
import play.util.io.ByteBufferInputStream
import java.lang.invoke.MethodHandles
import java.nio.ByteBuffer

/**
 *
 * @author LiangZengle
 */
class WireCodec<T : Message<*, *>>(val type: Class<T>) : Codec<T> {
  @Suppress("UNCHECKED_CAST")
  private val adapter: ProtoAdapter<T> =
    MethodHandles.publicLookup().findStaticVarHandle(type, "ADAPTER", ProtoAdapter::class.java)
      .get() as ProtoAdapter<T>

  override fun encode(value: T?): ByteArray {
    if (value == null) {
      return EmptyByteArray
    }
    return adapter.encode(value)
  }

  override fun decode(bytes: ByteArray): T {
    return adapter.decode(bytes)
  }

  fun decode(buffer: ByteBuffer): T {
    return adapter.decode(ByteBufferInputStream(buffer))
  }
}
