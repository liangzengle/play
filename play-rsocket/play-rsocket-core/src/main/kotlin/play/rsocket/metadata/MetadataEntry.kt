package play.rsocket.metadata

import io.netty.buffer.ByteBuf
import io.rsocket.metadata.CompositeMetadata

/**
 *
 *
 * @author LiangZengle
 */
interface MetadataEntry : CompositeMetadata.Entry {

  override fun getMimeType(): String

  fun parseFrom(data: ByteBuf): MetadataEntry
}
