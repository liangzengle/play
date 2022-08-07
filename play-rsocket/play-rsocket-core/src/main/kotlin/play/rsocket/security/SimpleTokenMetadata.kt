package play.rsocket.security

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.ByteBufUtil
import io.rsocket.metadata.WellKnownMimeType
import play.rsocket.metadata.MetadataEntry

/**
 *
 *
 * @author LiangZengle
 */
class SimpleTokenMetadata(val token: String) : MetadataEntry {

  companion object Default {
    fun parseFrom(data: ByteBuf): SimpleTokenMetadata {
      var index = 0
      val len = data.getByte(index)
      index++
      val token = data.getCharSequence(index, len.toInt(), Charsets.UTF_8)
      return SimpleTokenMetadata(token.toString())
    }
  }

  override fun getMimeType(): String {
    return WellKnownMimeType.MESSAGE_RSOCKET_AUTHENTICATION.string
  }

  override fun parseFrom(data: ByteBuf): SimpleTokenMetadata {
    return Default.parseFrom(data)
  }

  override fun getContent(): ByteBuf {
    val len = ByteBufUtil.utf8Bytes(token)
    check(len < Byte.MAX_VALUE) { "token too long" }
    val buffer = ByteBufAllocator.DEFAULT.buffer(len + 1)
    buffer.writeByte(len)
    ByteBufUtil.reserveAndWriteUtf8(buffer, token, len)
    return buffer
  }
}
