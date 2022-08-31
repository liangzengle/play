package play.rsocket.serializer.kryo

import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import play.kryo.PlayKryo
import play.kryo.util.TypeUtil
import play.rsocket.serializer.RSocketSerializer
import java.io.InputStream
import java.io.OutputStream
import java.lang.reflect.Type

/**
 *
 * @author LiangZengle
 */
class KryoSerializer(private val kryo: PlayKryo) : RSocketSerializer {

  override fun writeBoolean(out: OutputStream, value: Boolean) {
    (out as Output).writeBoolean(value)
  }

  override fun writeByte(out: OutputStream, value: Byte) {
    (out as Output).writeByte(value)
  }

  override fun writeShort(out: OutputStream, value: Short) {
    (out as Output).writeShort(value.toInt())
  }

  override fun writeInt(out: OutputStream, value: Int) {
    (out as Output).writeInt(value, true)
  }

  override fun writeLong(out: OutputStream, value: Long) {
    (out as Output).writeLong(value, true)
  }

  override fun writeFloat(out: OutputStream, value: Float) {
    (out as Output).writeFloat(value)
  }

  override fun writeDouble(out: OutputStream, value: Double) {
    (out as Output).writeDouble(value)
  }

  override fun writeUtf8(out: OutputStream, value: String?) {
    (out as Output).writeString(value)
  }

  override fun writeObject(out: OutputStream, type: Type, value: Any) {
    kryo.writeObject(out as Output, value, kryo.getSerializer(type))
  }

  override fun writeObjectOrNull(out: OutputStream, type: Type, value: Any?) {
    kryo.writeObjectOrNull(out as Output, value, kryo.getSerializer(type))
  }

  override fun writeBooleans(out: OutputStream, value: BooleanArray) {
    out as Output
    out.writeInt(value.size, true)
    out.writeBooleans(value, 0, value.size)
  }

  override fun writeBytes(out: OutputStream, value: ByteArray) {
    out as Output
    out.writeInt(value.size, true)
    out.writeBytes(value, 0, value.size)
  }

  override fun writeShorts(out: OutputStream, value: ShortArray) {
    out as Output
    out.writeInt(value.size, true)
    out.writeShorts(value, 0, value.size)
  }

  override fun writeInts(out: OutputStream, value: IntArray) {
    out as Output
    out.writeInt(value.size, true)
    out.writeInts(value, 0, value.size, true)
  }

  override fun writeLongs(out: OutputStream, value: LongArray) {
    out as Output
    out.writeInt(value.size, true)
    out.writeLongs(value, 0, value.size, true)
  }

  override fun writeFloats(out: OutputStream, value: FloatArray) {
    out as Output
    out.writeInt(value.size, true)
    out.writeFloats(value, 0, value.size)
  }

  override fun writeDoubles(out: OutputStream, value: DoubleArray) {
    out as Output
    out.writeInt(value.size, true)
    out.writeDoubles(value, 0, value.size)
  }

  override fun writeBooleans(out: OutputStream, value: BooleanArray, offset: Int, count: Int) {
    out as Output
    out.writeInt(count, true)
    out.writeBooleans(value, offset, count)
  }

  override fun writeBytes(out: OutputStream, value: ByteArray, offset: Int, count: Int) {
    out as Output
    out.writeInt(count, true)
    out.writeBytes(value, offset, count)
  }

  override fun writeShorts(out: OutputStream, value: ShortArray, offset: Int, count: Int) {
    out as Output
    out.writeInt(count, true)
    out.writeShorts(value, offset, count)
  }

  override fun writeInts(out: OutputStream, value: IntArray, offset: Int, count: Int) {
    out as Output
    out.writeInt(count, true)
    out.writeInts(value, offset, count, true)
  }

  override fun writeLongs(out: OutputStream, value: LongArray, offset: Int, count: Int) {
    out as Output
    out.writeInt(count, true)
    out.writeLongs(value, offset, count, true)
  }

  override fun writeFloats(out: OutputStream, value: FloatArray, offset: Int, count: Int) {
    out as Output
    out.writeInt(count, true)
    out.writeFloats(value, offset, count)
  }

  override fun writeDoubles(out: OutputStream, value: DoubleArray, offset: Int, count: Int) {
    out as Output
    out.writeInt(count, true)
    out.writeDoubles(value, offset, count)
  }

  override fun readBoolean(`in`: InputStream): Boolean {
    return (`in` as Input).readBoolean()
  }

  override fun readByte(`in`: InputStream): Byte {
    return (`in` as Input).readByte()
  }

  override fun readShort(`in`: InputStream): Short {
    return (`in` as Input).readShort()
  }

  override fun readInt(`in`: InputStream): Int {
    return (`in` as Input).readInt(true)
  }

  override fun readLong(`in`: InputStream): Long {
    return (`in` as Input).readLong(true)
  }

  override fun readFloat(`in`: InputStream): Float {
    return (`in` as Input).readFloat()
  }

  override fun readDouble(`in`: InputStream): Double {
    return (`in` as Input).readDouble()
  }

  override fun readUtf8(`in`: InputStream): String? {
    return (`in` as Input).readString()
  }

  @Suppress("UNCHECKED_CAST")
  override fun <T : Any> readObject(`in`: InputStream, type: Type): T {
    val rawClass: Class<*> = TypeUtil.getRawClass(type)
    return kryo.readObject(`in` as Input, rawClass, kryo.getSerializer(type)) as T
  }

  @Suppress("UNCHECKED_CAST")
  override fun <T : Any> readObjectOrNull(`in`: InputStream, type: Type): T? {
    val rawClass: Class<*> = TypeUtil.getRawClass(type)
    return kryo.readObjectOrNull(`in` as Input, rawClass, kryo.getSerializer(type)) as T?
  }

  override fun readBooleans(`in`: InputStream): BooleanArray {
    `in` as Input
    val len = `in`.readInt(true)
    return `in`.readBooleans(len)
  }

  override fun readBytes(`in`: InputStream): ByteArray {
    `in` as Input
    val len = `in`.readInt(true)
    return `in`.readBytes(len)
  }

  override fun readShorts(`in`: InputStream): ShortArray {
    `in` as Input
    val len = `in`.readInt(true)
    return `in`.readShorts(len)
  }

  override fun readInts(`in`: InputStream): IntArray {
    `in` as Input
    val len = `in`.readInt(true)
    return `in`.readInts(len, true)
  }

  override fun readLongs(`in`: InputStream): LongArray {
    `in` as Input
    val len = `in`.readInt(true)
    return `in`.readLongs(len, true)
  }

  override fun readFloats(`in`: InputStream): FloatArray {
    `in` as Input
    val len = `in`.readInt(true)
    return `in`.readFloats(len)
  }

  override fun readDoubles(`in`: InputStream): DoubleArray {
    `in` as Input
    val len = `in`.readInt(true)
    return `in`.readDoubles(len)
  }
}
