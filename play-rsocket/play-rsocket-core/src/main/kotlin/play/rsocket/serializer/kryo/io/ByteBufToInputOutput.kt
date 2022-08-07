package play.rsocket.serializer.kryo.io

import io.netty.buffer.ByteBuf
import play.rsocket.serializer.ByteBufToIOStreamAdapter

/**
 *
 *
 * @author LiangZengle
 */
object ByteBufToInputOutput : ByteBufToIOStreamAdapter {
  override fun toOutputStream(buffer: ByteBuf): ByteBufOutput {
    return ByteBufOutput(buffer)
  }

  override fun toInputStream(buffer: ByteBuf): ByteBufInput {
    return ByteBufInput(buffer)
  }
}
