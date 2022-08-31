package play.rsocket.serializer

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import java.lang.reflect.Type

/**
 *
 *
 * @author LiangZengle
 */
class RSocketCodec(private val provider: RSocketSerializerProvider, private val adapter: ByteBufToIOStreamAdapter) {

  fun encode(value: Any): ByteBuf {
    val serializer = provider.get()
    val buffer = ByteBufAllocator.DEFAULT.buffer()
    val outputStream = adapter.toOutputStream(buffer)
    RSocketSerializer.write(serializer, outputStream, value.javaClass, value)
    return buffer
  }

  fun decode(data: ByteBuf, type: Type): Any {
    val serializer = provider.get()
    val inputStream = adapter.toInputStream(data)
    return RSocketSerializer.read(serializer, inputStream, type)
  }
}
