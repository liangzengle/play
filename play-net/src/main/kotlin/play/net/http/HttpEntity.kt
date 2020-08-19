package play.net.http

import io.vavr.control.Option
import io.vavr.kotlin.some
import play.util.collection.EmptyByteArray

sealed class HttpEntity {
  abstract fun contentType(): Option<String>
  abstract fun contentLength(): Option<Int>

  companion object {
    @JvmStatic
    val empty: Strict = Strict(EmptyByteArray)
  }

  class Strict(val data: ByteArray, private val contentType: Option<String>) : HttpEntity() {
    constructor(data: String) : this(data.toByteArray(), some("text/plain"))
    constructor(data: ByteArray) : this(data, some("application/octed-stream"))

    override fun contentType(): Option<String> = contentType
    override fun contentLength(): Option<Int> = some(data.size)

    override fun toString(): String {
      return when {
        data.isEmpty() -> "<empty>"
        contentType.contains("application/octed-stream") -> data.contentToString()
        else -> data.toString(Charsets.UTF_8)
      }
    }
  }

  abstract class Stream : HttpEntity() {
    override fun contentType(): Option<String> = some("application/octet-stream")

    override fun contentLength(): Option<Int> = Option.none()

    override fun toString(): String {
      return "<streamed>"
    }
  }
}

