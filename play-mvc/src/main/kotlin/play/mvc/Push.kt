package play.mvc

import mu.KLogging
import play.codec.MessageCodec
import play.util.EmptyByteArray
import play.util.json.Json

class Push<T>(val msgId: MsgId) {
  companion object : KLogging()

  constructor(moduleId: Short, cmd: Byte) : this(MsgId(moduleId, cmd))

  @JvmName("of")
  operator fun invoke(message: T?): Response {
    return Response(Header(msgId), 0, encode(message))
  }

  private fun encode(message: Any?): ByteArray {
    return try {
      MessageCodec.encode(message)
    } catch (e: Exception) {
      logger.error(e) { "encode message error: ${toString(message)}" }
      return EmptyByteArray
    }
  }

  private fun toString(message: Any?): String {
    return if (message == null) "null" else {
      "${message.javaClass.simpleName}(${Json.stringify(message)})"
    }
  }
}
