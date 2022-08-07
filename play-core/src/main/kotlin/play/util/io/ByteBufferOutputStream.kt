package play.util.io

import java.io.OutputStream
import java.nio.ByteBuffer

/**
 *
 * @author LiangZengle
 */
class ByteBufferOutputStream(private val buffer: ByteBuffer) : OutputStream() {
  override fun write(b: Int) {
    buffer.put(b.toByte())
  }

  override fun write(b: ByteArray, off: Int, len: Int) {
    buffer.put(b, off, len)
  }
}
