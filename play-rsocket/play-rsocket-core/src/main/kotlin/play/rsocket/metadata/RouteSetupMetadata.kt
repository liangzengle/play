package play.rsocket.metadata

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator

/**
 *
 *
 * @author LiangZengle
 */
class RouteSetupMetadata(val nodeId: Int, val role: Byte) : MetadataEntry {

  companion object {
    @JvmStatic
    val DefaultInstance = RouteSetupMetadata(0, 0)
  }

  override fun parseFrom(data: ByteBuf): RouteSetupMetadata {
    var offset = 0
    val nodeId = data.getInt(offset)
    offset += 4
    val role = data.getByte(offset)
    return RouteSetupMetadata(nodeId, role)
  }

  override fun getContent(): ByteBuf {
    val buffer = ByteBufAllocator.DEFAULT.buffer(5)
    buffer.writeInt(nodeId)
    buffer.writeByte(role.toInt())
    return buffer
  }

  override fun getMimeType(): String {
    return MimeTypes.RouteSetup
  }

  override fun toString(): String {
    return "RouteSetupMetadata(nodeId=$nodeId, role=$role)"
  }
}
