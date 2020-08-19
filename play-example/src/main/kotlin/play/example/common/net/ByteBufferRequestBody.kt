package play.example.common.net

import com.google.common.primitives.Ints
import com.google.common.primitives.Longs
import play.mvc.RequestBody
import play.util.primitive.toBoolean
import java.nio.ByteBuffer

/**
 *
 * @author LiangZengle
 */
class ByteBufferRequestBody(array: ByteArray) : RequestBody {
  private val buffer = ByteBuffer.wrap(array)
  override fun reset() {
    buffer.position(0)
  }

  override fun readBoolean(): Boolean = buffer.get().toBoolean()

  override fun readInt(): Int = buffer.int

  override fun readLong(): Long = buffer.long

  override fun readString(): String {
    val len = buffer.get()
    if (len < 0) throw IllegalArgumentException()
    val arr = ByteArray(len.toInt())
    buffer.get(arr)
    return String(arr)
  }

  override fun readByteArray(): ByteArray {
    return buffer.array().copyOfRange(buffer.position(), buffer.limit())
  }

  override fun readIntList(): List<Int> {
    val n = buffer.get().toInt()
    if (n < 0 || n > 32) throw IllegalArgumentException()
    val result = IntArray(n)
    for (i in 0..n) {
      result[i] = buffer.int
    }
    return Ints.asList(*result)
  }

  override fun readLongList(): List<Long> {
    val n = buffer.get().toInt()
    if (n < 0 || n > 32) throw IllegalArgumentException()
    val result = LongArray(n)
    for (i in 0..n) {
      result[i] = buffer.long
    }
    return Longs.asList(*result)
  }

  override fun toByteArray(): ByteArray {
    return buffer.array()
  }
}
