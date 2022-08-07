package play.rsocket.metadata

import io.netty.buffer.ByteBuf
import io.rsocket.ConnectionSetupPayload
import io.rsocket.Payload
import io.rsocket.metadata.CompositeMetadata
import io.rsocket.metadata.WellKnownMimeType
import java.util.*

/**
 *
 *
 * @author LiangZengle
 */
object MetadataExtractor {

  @JvmStatic
  fun <T> extract(payload: ConnectionSetupPayload, mimeType: String, decoder: (ByteBuf) -> T): T? {
    return extract(payload, payload.metadataMimeType(), mimeType, decoder)
  }

  @Suppress("UNCHECKED_CAST")
  @JvmStatic
  fun <T : MetadataEntry> extract(payload: ConnectionSetupPayload, defaultInstance: T): T? {
    return extract(payload, payload.metadataMimeType(), defaultInstance.mimeType) {
      defaultInstance.parseFrom(it) as T
    }
  }

  @JvmStatic
  fun <T> extract(payload: Payload, metadataMimeType: String, mimeType: String, decoder: (ByteBuf) -> T): T? {
    if (metadataMimeType == WellKnownMimeType.MESSAGE_RSOCKET_COMPOSITE_METADATA.string) {
      for (entry in CompositeMetadata(payload.metadata(), false)) {
        if (entry.mimeType == mimeType) {
          return decoder(entry.content)
        }
      }
    } else if (metadataMimeType == mimeType) {
      return decoder(payload.sliceMetadata())
    }
    return null
  }

  @JvmStatic
  fun <T : MetadataEntry> extract(payload: Payload, defaultInstance: T): T? {
    return extract(payload, WellKnownMimeType.MESSAGE_RSOCKET_COMPOSITE_METADATA.string, defaultInstance)
  }

  @Suppress("UNCHECKED_CAST")
  @JvmStatic
  fun <T : MetadataEntry> extract(payload: Payload, metadataMimeType: String, defaultInstance: T): T? {
    return extract(
      payload,
      metadataMimeType,
      defaultInstance.mimeType
    ) { defaultInstance.parseFrom(it) as T }
  }

  @JvmStatic
  fun extract(payload: Payload, metadataMimeType: String, decoder: (String, ByteBuf) -> Any): Map<String, Any> {
    if (metadataMimeType == WellKnownMimeType.MESSAGE_RSOCKET_COMPOSITE_METADATA.string) {
      val result = hashMapOf<String, Any>()
      for (entry in CompositeMetadata(payload.metadata(), false)) {
        val mimeType = entry.mimeType!!
        val value = decoder(mimeType, entry.content)
        result[mimeType] = value
      }
      return result
    }
    val value = decoder(metadataMimeType, payload.sliceMetadata())
    return Collections.singletonMap(metadataMimeType, value)
  }
}
