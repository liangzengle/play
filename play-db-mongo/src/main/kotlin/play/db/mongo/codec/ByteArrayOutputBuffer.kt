package play.db.mongo.codec

import org.bson.ByteBuf
import org.bson.ByteBufNIO
import org.bson.io.OutputBuffer
import play.util.collection.asSingletonList
import java.io.OutputStream
import java.nio.ByteBuffer

internal class ByteArrayOutputBuffer(initialCapacity: Int = 256) : OutputBuffer() {
  private val buffer = ByteBufNIO(ByteBuffer.allocate(initialCapacity))
  private val bufferList = buffer.asSingletonList()

  override fun write(position: Int, value: Int) {
    buffer.put(position, (0xFF and value).toByte())
  }

  override fun getPosition(): Int {
    return buffer.asNIO().position()
  }

  override fun getSize(): Int {
    return buffer.asNIO().position()
  }

  override fun truncateToPosition(newPosition: Int) {
    buffer.asNIO().position(newPosition)
  }

  override fun writeBytes(bytes: ByteArray, offset: Int, length: Int) {
    buffer.put(bytes, offset, length)
  }

  override fun writeByte(value: Int) {
    buffer.put((0xFF and value).toByte())
  }

  override fun pipe(out: OutputStream): Int {
    if (position == 0) {
      return 0
    }
    out.write(buffer.array(), 0, position)
    return position
  }

  override fun getByteBuffers(): List<ByteBuf> {
    return bufferList
  }

  internal fun array(): ByteArray = buffer.array()

  internal fun release() {
    buffer.clear()
  }
}
