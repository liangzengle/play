package play.wire

import com.squareup.wire.*
import java.io.IOException

/**
 *
 * @author LiangZengle
 */
internal object PlayProtoAdapter {

  //  private const val FIXED_BOOL_SIZE = 1
  private const val FIXED_32_SIZE = 4
  private const val FIXED_64_SIZE = 8

  val INT32 = IntProtoAdapter
  val INT64 = LongProtoAdapter
  val FLOAT = FloatProtoAdapter
  val DOUBLE = DoubleProtoAdapter

  internal object IntProtoAdapter : ProtoAdapter<Int>(
    FieldEncoding.VARINT,
    Int::class,
    null,
    Syntax.PROTO_2,
    0
  ) {
    override fun encodedSize(value: Int): Int = int32Size(value)

    @Throws(IOException::class)
    override fun encode(writer: ProtoWriter, value: Int) {
      writer.writeSignedVarint32_(value)
    }

    @Throws(IOException::class)
    override fun encode(writer: ReverseProtoWriter, value: Int) {
      writer.writeSignedVarint32_(value)
    }

    @Throws(IOException::class)
    override fun decode(reader: ProtoReader): Int = reader.readVarint32()

    @Throws(IOException::class)
    fun decodeAsInt(reader: ProtoReader): Int = reader.readVarint32()

    override fun redact(value: Int): Int = throw UnsupportedOperationException()
  }

  internal object LongProtoAdapter : ProtoAdapter<Long>(
    FieldEncoding.VARINT,
    Long::class,
    null,
    Syntax.PROTO_2,
    0L
  ) {
    override fun encodedSize(value: Long): Int = varint64Size(value)

    @Throws(IOException::class)
    override fun encode(writer: ProtoWriter, value: Long) {
      writer.writeVarint64(value)
    }

    @Throws(IOException::class)
    override fun encode(writer: ReverseProtoWriter, value: Long) {
      writer.writeVarint64(value)
    }

    @Throws(IOException::class)
    override fun decode(reader: ProtoReader): Long = reader.readVarint64()

    @Throws(IOException::class)
    fun decodeAsLong(reader: ProtoReader): Long = reader.readVarint64()

    override fun redact(value: Long): Long = throw UnsupportedOperationException()
  }

  internal object FloatProtoAdapter : ProtoAdapter<Float>(
    FieldEncoding.FIXED32,
    Float::class,
    null,
    Syntax.PROTO_2,
    0.0f
  ) {
    override fun encodedSize(value: Float): Int = FIXED_32_SIZE

    @Throws(IOException::class)
    override fun encode(writer: ProtoWriter, value: Float) {
      writer.writeFixed32(value.toBits())
    }

    @Throws(IOException::class)
    override fun encode(writer: ReverseProtoWriter, value: Float) {
      writer.writeFixed32(value.toBits())
    }

    @Throws(IOException::class)
    override fun decode(reader: ProtoReader): Float {
      return Float.fromBits(reader.readFixed32())
    }

    @Throws(IOException::class)
    fun decodeAsFloat(reader: ProtoReader): Float {
      return Float.fromBits(reader.readFixed32())
    }

    override fun redact(value: Float): Float = throw UnsupportedOperationException()
  }

  internal object DoubleProtoAdapter : ProtoAdapter<Double>(
    FieldEncoding.FIXED64,
    Double::class,
    null,
    Syntax.PROTO_2,
    0.0
  ) {
    override fun encodedSize(value: Double): Int = FIXED_64_SIZE

    @Throws(IOException::class)
    override fun encode(writer: ProtoWriter, value: Double) {
      writer.writeFixed64(value.toBits())
    }

    @Throws(IOException::class)
    override fun encode(writer: ReverseProtoWriter, value: Double) {
      writer.writeFixed64(value.toBits())
    }

    @Throws(IOException::class)
    override fun decode(reader: ProtoReader): Double {
      return Double.fromBits(reader.readFixed64())
    }

    @Throws(IOException::class)
    fun decodeAsDouble(reader: ProtoReader): Double {
      return Double.fromBits(reader.readFixed64())
    }

    override fun redact(value: Double): Double = throw UnsupportedOperationException()
  }

  private fun ProtoWriter.writeSignedVarint32_(value: Int) {
    if (value >= 0) {
      writeVarint32(value)
    } else {
      // Must sign-extend.
      writeVarint64(value.toLong())
    }
  }

  fun ReverseProtoWriter.writeSignedVarint32_(value: Int) {
    if (value >= 0) {
      writeVarint32(value)
    } else {
      // Must sign-extend.
      writeVarint64(value.toLong())
    }
  }

  /**
   * Computes the number of bytes that would be needed to encode a signed variable-length integer
   * of up to 32 bits.
   */
  internal fun int32Size(value: Int): Int {
    return if (value >= 0) {
      varint32Size(value)
    } else {
      // Must sign-extend.
      10
    }
  }

  /**
   * Compute the number of bytes that would be needed to encode a varint. `value` is treated
   * as unsigned, so it won't be sign-extended if negative.
   */
  internal fun varint32Size(value: Int): Int {
    if (value and (-0x1 shl 7) == 0) return 1
    if (value and (-0x1 shl 14) == 0) return 2
    if (value and (-0x1 shl 21) == 0) return 3
    return if (value and (-0x1 shl 28) == 0) 4 else 5
  }

  /** Compute the number of bytes that would be needed to encode a varint. */
  internal fun varint64Size(value: Long): Int {
    if (value and (-0x1L shl 7) == 0L) return 1
    if (value and (-0x1L shl 14) == 0L) return 2
    if (value and (-0x1L shl 21) == 0L) return 3
    if (value and (-0x1L shl 28) == 0L) return 4
    if (value and (-0x1L shl 35) == 0L) return 5
    if (value and (-0x1L shl 42) == 0L) return 6
    if (value and (-0x1L shl 49) == 0L) return 7
    if (value and (-0x1L shl 56) == 0L) return 8
    return if (value and (-0x1L shl 63) == 0L) 9 else 10
  }
}
