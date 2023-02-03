package play.codec.wire

import com.squareup.wire.ProtoAdapter
import okio.ByteString
import java.io.OutputStream

/**
 *
 * @author LiangZengle
 */
object Wire {

  @JvmStatic
  fun getInternalArray(byteString: ByteString): ByteArray {
    val out = ThiefOutputStream()
    byteString.write(out)
    return out.get()
  }

  private class ThiefOutputStream : OutputStream() {
    private lateinit var value: ByteArray
    override fun write(b: Int) {
      throw UnsupportedOperationException()
    }

    override fun write(b: ByteArray) {
      value = b
    }

    fun get() = value
  }

  @JvmStatic
  fun <T> getBuiltinAdapter(type: Class<T>): ProtoAdapter<T> {
    val adapter = when (type.canonicalName) {
      "java.lang.Boolean", "boolean" -> ProtoAdapter.BOOL
      "java.lang.Integer", "int" -> ProtoAdapter.INT32
      "java.lang.Long", "long" -> ProtoAdapter.INT64
      "java.lang.Float", "float" -> ProtoAdapter.FLOAT
      "java.lang.Double", "double" -> ProtoAdapter.DOUBLE
      "byte[]" -> ProtoAdapter.BYTES
      "java.lang.String" -> ProtoAdapter.STRING
      "java.time.Duration" -> ProtoAdapter.DURATION
      "com.google.protobuf.BoolValue" -> ProtoAdapter.BOOL_VALUE
      "com.google.protobuf.Int32Value" -> ProtoAdapter.INT32_VALUE
      "com.google.protobuf.Int64Value" -> ProtoAdapter.INT64_VALUE
      "com.google.protobuf.FloatValue" -> ProtoAdapter.FLOAT_VALUE
      "com.google.protobuf.DoubleValue" -> ProtoAdapter.DOUBLE_VALUE
      "com.google.protobuf.StringValue" -> ProtoAdapter.STRING_VALUE
      "com.google.protobuf.BytesValue" -> ProtoAdapter.BYTES_VALUE
      "com.google.protobuf.UInt32Value" -> ProtoAdapter.UINT32_VALUE
      "com.google.protobuf.UInt64Value" -> ProtoAdapter.UINT64_VALUE
      "com.google.protobuf.Struct" -> ProtoAdapter.STRUCT_MAP
      "com.google.protobuf.Value" -> ProtoAdapter.STRUCT_VALUE
      "com.google.protobuf.ListValue" -> ProtoAdapter.STRUCT_LIST
      "com.google.protobuf.NullValue" -> ProtoAdapter.STRUCT_NULL
      else -> throw IllegalArgumentException("Maybe not builtin type: ${type.name}")
    }
    @Suppress("UNCHECKED_CAST")
    return adapter as ProtoAdapter<T>
  }
}
