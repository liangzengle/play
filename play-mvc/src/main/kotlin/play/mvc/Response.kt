package play.mvc

import play.codec.MessageCodec
import play.util.EmptyByteArray

class Response constructor(
  val header: Header,
  @JvmField val statusCode: Int,
  @JvmField val body: ByteArray = EmptyByteArray
) {
  constructor(header: Header, statusCode: Int, data: Any?) : this(header, statusCode, MessageCodec.encode(data))

  override fun toString(): String {
    return "Response(header=$header, statusCode=$statusCode, body=${body.contentToString()})"
  }
}
