package play.util.io

import play.util.min
import java.io.InputStream
import java.nio.ByteBuffer

/**
 *
 * @author LiangZengle
 */
class ByteBufferInputStream(private val buffer: ByteBuffer) : InputStream() {
  override fun read(): Int {
    return if (!buffer.hasRemaining()) -1 else buffer.get().toInt() and 0xFF
  }

  override fun read(b: ByteArray, off: Int, len: Int): Int {
    if (!buffer.hasRemaining()) return -1
    val count = len.min(buffer.remaining())
    buffer.get(b, off, count)
    return count
  }
}
