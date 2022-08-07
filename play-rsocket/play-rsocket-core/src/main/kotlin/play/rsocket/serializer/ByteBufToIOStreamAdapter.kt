package play.rsocket.serializer

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufInputStream
import io.netty.buffer.ByteBufOutputStream
import java.io.InputStream
import java.io.OutputStream

/**
 *
 * @author LiangZengle
 */
interface ByteBufToIOStreamAdapter {

  fun toOutputStream(buffer: ByteBuf): OutputStream

  fun toInputStream(buffer: ByteBuf): InputStream

  companion object Default : ByteBufToIOStreamAdapter {
    override fun toOutputStream(buffer: ByteBuf): OutputStream {
      return ByteBufOutputStream(buffer)
    }

    override fun toInputStream(buffer: ByteBuf): InputStream {
      return ByteBufInputStream(buffer)
    }
  }
}
