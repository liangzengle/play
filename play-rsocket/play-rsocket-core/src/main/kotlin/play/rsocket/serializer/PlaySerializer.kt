package play.rsocket.serializer

import java.io.InputStream
import java.io.OutputStream
import java.lang.reflect.Type
import kotlin.reflect.KType
import kotlin.reflect.jvm.javaType

/**
 *
 * @author LiangZengle
 */
interface PlaySerializer {

  companion object {
    @JvmStatic
    fun write(serializer: PlaySerializer, out: OutputStream, type: Type, value: Any) {
      when (value.javaClass.canonicalName) {
        "boolean", "java.lang.Boolean" -> serializer.writeBoolean(out, value as Boolean)
        "byte", "java.lang.Byte" -> serializer.writeByte(out, value as Byte)
        "short", "java.lang.Short" -> serializer.writeShort(out, value as Short)
        "int", "java.lang.Integer" -> serializer.writeInt(out, value as Int)
        "long", "java.lang.Long" -> serializer.writeLong(out, value as Long)
        "float", "java.lang.Float" -> serializer.writeFloat(out, value as Float)
        "double", "java.lang.Double" -> serializer.writeDouble(out, value as Double)
        "boolean[]" -> serializer.writeBooleans(out, value as BooleanArray)
        "byte[]" -> serializer.writeBytes(out, value as ByteArray)
        "short[]" -> serializer.writeShorts(out, value as ShortArray)
        "int[]" -> serializer.writeInts(out, value as IntArray)
        "long[]" -> serializer.writeLongs(out, value as LongArray)
        "float[]" -> serializer.writeFloats(out, value as FloatArray)
        "double[]" -> serializer.writeDoubles(out, value as DoubleArray)
        else -> serializer.writeObject(out, type, value)
      }
    }

    @JvmStatic
    fun read(serializer: PlaySerializer, input: InputStream, type: Type): Any {
      return if (type is Class<*>) {
        when (type.canonicalName) {
          "boolean", "java.lang.Boolean" -> serializer.readBoolean(input)
          "byte", "java.lang.Byte" -> serializer.readByte(input)
          "short", "java.lang.Short" -> serializer.readShort(input)
          "int", "java.lang.Integer" -> serializer.readInt(input)
          "long", "java.lang.Long" -> serializer.readLong(input)
          "float", "java.lang.Float" -> serializer.readFloat(input)
          "double", "java.lang.Double" -> serializer.readDouble(input)
          "boolean[]" -> serializer.readBooleans(input)
          "byte[]" -> serializer.readBytes(input)
          "short[]" -> serializer.readShorts(input)
          "int[]" -> serializer.readInts(input)
          "long[]" -> serializer.readLongs(input)
          "float[]" -> serializer.readFloats(input)
          "double[]" -> serializer.readDoubles(input)
          else -> serializer.readObject(input, type)
        }
      } else {
        serializer.readObject(input, type)
      }
    }
  }


  fun writeBoolean(out: OutputStream, value: Boolean)
  fun writeByte(out: OutputStream, value: Byte)
  fun writeShort(out: OutputStream, value: Short)
  fun writeInt(out: OutputStream, value: Int)
  fun writeLong(out: OutputStream, value: Long)
  fun writeFloat(out: OutputStream, value: Float)
  fun writeDouble(out: OutputStream, value: Double)
  fun writeUtf8(out: OutputStream, value: String?)
  fun writeObject(out: OutputStream, type: Type, value: Any)
  fun writeObject(out: OutputStream, type: KType, value: Any) = writeObject(out, type.javaType, value)
  fun writeObjectOrNull(out: OutputStream, type: Type, value: Any?)
  fun writeObjectOrNull(out: OutputStream, type: KType, value: Any?) = writeObjectOrNull(out, type.javaType, value)

  fun writeBooleans(out: OutputStream, value: BooleanArray)
  fun writeBytes(out: OutputStream, value: ByteArray)
  fun writeShorts(out: OutputStream, value: ShortArray)
  fun writeInts(out: OutputStream, value: IntArray)
  fun writeLongs(out: OutputStream, value: LongArray)
  fun writeFloats(out: OutputStream, value: FloatArray)
  fun writeDoubles(out: OutputStream, value: DoubleArray)

  fun writeBooleans(out: OutputStream, value: BooleanArray, offset: Int, count: Int)
  fun writeBytes(out: OutputStream, value: ByteArray, offset: Int, count: Int)
  fun writeShorts(out: OutputStream, value: ShortArray, offset: Int, count: Int)
  fun writeInts(out: OutputStream, value: IntArray, offset: Int, count: Int)
  fun writeLongs(out: OutputStream, value: LongArray, offset: Int, count: Int)
  fun writeFloats(out: OutputStream, value: FloatArray, offset: Int, count: Int)
  fun writeDoubles(out: OutputStream, value: DoubleArray, offset: Int, count: Int)

  fun readBoolean(`in`: InputStream): Boolean
  fun readByte(`in`: InputStream): Byte
  fun readShort(`in`: InputStream): Short
  fun readInt(`in`: InputStream): Int
  fun readLong(`in`: InputStream): Long
  fun readFloat(`in`: InputStream): Float
  fun readDouble(`in`: InputStream): Double
  fun readUtf8(`in`: InputStream): String?
  fun <T : Any> readObject(`in`: InputStream, type: Type): T
  fun <T : Any> readObject(`in`: InputStream, type: KType): T = readObject(`in`, type.javaType)
  fun <T : Any> readObject(`in`: InputStream, type: Class<T>): T = readObject(`in`, type as Type)

  fun <T : Any> readObjectOrNull(`in`: InputStream, type: Type): T?
  fun <T : Any> readObjectOrNull(`in`: InputStream, type: KType): T? = readObjectOrNull(`in`, type.javaType)
  fun <T : Any> readObjectOrNull(`in`: InputStream, type: Class<T>): T? = readObjectOrNull(`in`, type as Type)

  fun readBooleans(`in`: InputStream): BooleanArray
  fun readBytes(`in`: InputStream): ByteArray
  fun readShorts(`in`: InputStream): ShortArray
  fun readInts(`in`: InputStream): IntArray
  fun readLongs(`in`: InputStream): LongArray
  fun readFloats(`in`: InputStream): FloatArray
  fun readDoubles(`in`: InputStream): DoubleArray
}
