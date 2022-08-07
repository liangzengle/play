package play.rsocket.serializer.kryo

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import play.rsocket.serializer.PlaySerializer
import play.rsocket.serializer.kryo.io.ByteBufInput
import play.rsocket.serializer.kryo.io.ByteBufOutput
import java.lang.reflect.Type

/**
 *
 * @author LiangZengle
 */
object KryoCodec {
  val decoder: (ByteBuf, Type) -> Any? = { buffer, type ->
    val serializer = KryoSerializerProvider.get()
    val input = ByteBufInput(buffer)
    PlaySerializer.read(serializer, input, type)
  }

  val encoder: (Any) -> ByteBuf = { o ->
    val serializer = KryoSerializerProvider.get()
    val buffer = ByteBufAllocator.DEFAULT.buffer()
    val output = ByteBufOutput(buffer)
    PlaySerializer.write(serializer, output, o.javaClass, o)
    buffer
  }
}
