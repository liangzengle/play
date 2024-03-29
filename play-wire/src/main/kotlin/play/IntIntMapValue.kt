// Code generated by Wire protocol buffer compiler, do not edit.
// Source: play.IntIntMapValue in play.proto
package play

import com.squareup.wire.*
import com.squareup.wire.Syntax.PROTO_3
import okio.ByteString
import org.eclipse.collections.api.factory.primitive.IntIntMaps
import org.eclipse.collections.api.map.primitive.IntIntMap
import org.eclipse.collectionx.asJava
import org.eclipse.collectionx.ofAll

public class IntIntMapValue(
  value: IntIntMap = IntIntMaps.immutable.empty(),
  unknownFields: ByteString = ByteString.EMPTY,
) : Message<IntIntMapValue, Nothing>(ADAPTER, unknownFields) {
  constructor(
    value: Map<Int, Int> = emptyMap(),
    unknownFields: ByteString = ByteString.EMPTY
  ) : this(IntIntMaps.immutable.ofAll(value), unknownFields)

  @field:WireField(
    tag = 1,
    keyAdapter = "com.squareup.wire.ProtoAdapter#INT32",
    adapter = "com.squareup.wire.ProtoAdapter#INT32",
    declaredName = "value",
  )
  public val value: IntIntMap = value.toImmutable()

  @Deprecated(
    message = "Shouldn't be used in Kotlin",
    level = DeprecationLevel.HIDDEN,
  )
  public override fun newBuilder(): Nothing =
    throw AssertionError("Builders are deprecated and only available in a javaInterop build; see https://square.github.io/wire/wire_compiler/#kotlin")

  public override fun equals(other: Any?): Boolean {
    if (other === this) return true
    if (other !is IntIntMapValue) return false
    if (unknownFields != other.unknownFields) return false
    if (value != other.value) return false
    return true
  }

  public override fun hashCode(): Int {
    var result = super.hashCode
    if (result == 0) {
      result = unknownFields.hashCode()
      result = result * 37 + value.hashCode()
      super.hashCode = result
    }
    return result
  }

  public override fun toString(): String {
    val result = mutableListOf<String>()
    if (value.notEmpty()) result += """value_=$value"""
    return result.joinToString(prefix = "IntIntMapValue{", separator = ", ", postfix = "}")
  }

  public fun copy(
    value: IntIntMap = this.value, unknownFields: ByteString =
      this.unknownFields
  ): IntIntMapValue = IntIntMapValue(value, unknownFields)

  public companion object {

    @JvmStatic
    val EMPTY = IntIntMapValue(IntIntMaps.immutable.empty())

    @JvmField
    public val ADAPTER: ProtoAdapter<IntIntMapValue> = object : ProtoAdapter<IntIntMapValue>(
      FieldEncoding.LENGTH_DELIMITED,
      IntIntMapValue::class,
      "type.googleapis.com/play.IntIntMapValue",
      PROTO_3,
      null,
      "play.proto"
    ) {
      private val valueAdapter: ProtoAdapter<Map<Int, Int>> by lazy {
        ProtoAdapter.newMapAdapter(ProtoAdapter.INT32, ProtoAdapter.INT32)
      }

      public override fun encodedSize(`value`: IntIntMapValue): Int {
        var size = value.unknownFields.size
        size += valueAdapter.encodedSizeWithTag(1, value.value.asJava())
        return size
      }

      public override fun encode(writer: ProtoWriter, `value`: IntIntMapValue): Unit {
        valueAdapter.encodeWithTag(writer, 1, value.value.asJava())
        writer.writeBytes(value.unknownFields)
      }

      public override fun encode(writer: ReverseProtoWriter, `value`: IntIntMapValue): Unit {
        writer.writeBytes(value.unknownFields)
        valueAdapter.encodeWithTag(writer, 1, value.value.asJava())
      }

      public override fun decode(reader: ProtoReader): IntIntMapValue {
        val value = IntIntMaps.mutable.empty()
        val unknownFields = reader.forEachTag { tag ->
          when (tag) {
            1 -> valueAdapter.decode(reader).forEach(value::put)
            else -> reader.readUnknownField(tag)
          }
        }
        return IntIntMapValue(
          value = value,
          unknownFields = unknownFields
        )
      }

      public override fun redact(`value`: IntIntMapValue): IntIntMapValue = value.copy(
        unknownFields = ByteString.EMPTY
      )
    }

    private const val serialVersionUID: Long = 0L
  }
}
