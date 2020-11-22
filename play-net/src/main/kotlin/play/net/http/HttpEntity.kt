package play.net.http

import play.util.collection.EmptyByteArray
import play.util.contains
import java.util.*

sealed class HttpEntity {
  abstract fun contentType(): Optional<String>
  abstract fun contentLength(): OptionalInt

  companion object {
    @JvmStatic
    val empty: Strict = Strict(EmptyByteArray)
  }

  class Strict(val data: ByteArray, private val contentType: Optional<String>) : HttpEntity() {
    constructor(data: String) : this(data.toByteArray(), Optional.of("text/plain"))
    constructor(data: ByteArray) : this(data, Optional.of("application/octet-stream"))

    override fun contentType(): Optional<String> = contentType
    override fun contentLength(): OptionalInt = OptionalInt.of(data.size)

    override fun toString(): String {
      return when {
        data.isEmpty() -> "<empty>"
        contentType.contains("application/octet-stream") -> data.contentToString()
        else -> data.toString(Charsets.UTF_8)
      }
    }
  }

  abstract class Stream : HttpEntity() {
    override fun contentType(): Optional<String> = Optional.of("application/octet-stream")

    override fun contentLength(): OptionalInt = OptionalInt.empty()

    override fun toString(): String {
      return "<streamed>"
    }
  }
}
