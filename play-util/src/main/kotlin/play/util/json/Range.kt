package play.util.json

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.deser.std.PrimitiveArrayDeserializers
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import play.util.unsafeCast

internal object IntRangeSerializer : StdSerializer<IntRange>(IntRange::class.java) {
  override fun serialize(value: IntRange, gen: JsonGenerator, provider: SerializerProvider?) {
    gen.writeStartArray()
    gen.writeNumber(value.first)
    gen.writeNumber(value.last)
    gen.writeEndArray()
  }
}

internal object IntRangeDeserializer : StdDeserializer<IntRange>(IntRange::class.java) {
  override fun deserialize(p: JsonParser, ctxt: DeserializationContext): IntRange {
    val array = PrimitiveArrayDeserializers.forType(Int::class.java)
      .unsafeCast<JsonDeserializer<IntArray>>()
      .deserialize(p, ctxt)
    return IntRange(array[0], array[1])
  }
}

internal object LongRangeSerializer : StdSerializer<LongRange>(LongRange::class.java) {
  override fun serialize(value: LongRange, gen: JsonGenerator, provider: SerializerProvider?) {
    gen.writeStartArray()
    gen.writeNumber(value.first)
    gen.writeNumber(value.last)
    gen.writeEndArray()
  }
}

internal object LongRangeDeserializer : StdDeserializer<LongRange>(LongRange::class.java) {
  override fun deserialize(p: JsonParser, ctxt: DeserializationContext): LongRange {
    val array = PrimitiveArrayDeserializers.forType(Long::class.java)
      .unsafeCast<JsonDeserializer<LongArray>>()
      .deserialize(p, ctxt)
    return LongRange(array[0], array[1])
  }
}
