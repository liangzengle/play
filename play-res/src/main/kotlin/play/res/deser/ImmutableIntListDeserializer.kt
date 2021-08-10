package play.res.deser

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.deser.std.PrimitiveArrayDeserializers
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.datatype.eclipsecollections.deser.map.EclipseMapDeserializers
import com.fasterxml.jackson.datatype.eclipsecollections.deser.set.ImmutableSetDeserializer
import com.google.common.primitives.Ints
import com.google.common.primitives.Longs
import org.eclipse.collections.api.map.primitive.ImmutableIntIntMap
import org.eclipse.collections.api.map.primitive.ImmutableIntLongMap
import org.eclipse.collections.api.map.primitive.ImmutableLongIntMap
import org.eclipse.collections.api.map.primitive.ImmutableLongLongMap
import org.eclipse.collectionx.toJava
import play.util.unsafeCast

internal object PrimitiveImmutableCollectionDeserializers {

  fun forList(elemType: Class<*>): JsonDeserializer<*>? {
    if (elemType == Integer::class.java || elemType == Int::class.java) {
      return IntList
    }
    if (elemType == java.lang.Long::class.java || elemType == Long::class.java) {
      return LongList
    }
    return null
  }

  fun forSet(elemType: Class<*>): JsonDeserializer<*>? {
    if (elemType == Integer::class.java || elemType == Int::class.java) {
      return IntSet
    }
    if (elemType == java.lang.Long::class.java || elemType == Long::class.java) {
      return LongSet
    }
    return null
  }

  fun forMap(keyType: Class<*>, valueType: Class<*>): JsonDeserializer<*>? {
    if (keyType == Integer::class.java || keyType == Int::class.java) {
      if (valueType == Integer::class.java || valueType == Int::class.java) {
        return IntIntMap
      }
      if (valueType == java.lang.Long::class.java || valueType == Long::class.java) {
        return IntLongMap
      }
    }
    if (keyType == java.lang.Long::class.java || keyType == Long::class.java) {
      if (valueType == Integer::class.java || valueType == Int::class.java) {
        return LongIntMap
      }
      if (valueType == java.lang.Long::class.java || valueType == Long::class.java) {
        return LongLongMap
      }
    }
    return null
  }

  private val IntList = object : StdDeserializer<List<Int>>(List::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): List<Int> {
      val array =
        PrimitiveArrayDeserializers.forType(Int::class.java).unsafeCast<JsonDeserializer<IntArray>>()
          .deserialize(p, ctxt)
      return Ints.asList(*array)
    }
  }

  private val LongList = object : StdDeserializer<List<Long>>(List::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): List<Long> {
      val array =
        PrimitiveArrayDeserializers.forType(Long::class.java).unsafeCast<JsonDeserializer<LongArray>>()
          .deserialize(p, ctxt)
      return Longs.asList(*array)
    }
  }

  private val IntSet = object : StdDeserializer<Set<Int>>(Set::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Set<Int> {
      return ImmutableSetDeserializer.Int.INSTANCE.deserialize(p, ctxt).toJava()
    }
  }

  private val LongSet = object : StdDeserializer<Set<Long>>(Set::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Set<Long> {
      return ImmutableSetDeserializer.Long.INSTANCE.deserialize(p, ctxt).toJava()
    }
  }

  private val IntIntMap = object : StdDeserializer<Map<Int, Int>>(Map::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Map<Int, Int> {
      val type = ctxt.typeFactory.constructType(ImmutableIntIntMap::class.java)
      return EclipseMapDeserializers.createDeserializer(type).deserialize(p, ctxt).unsafeCast<ImmutableIntIntMap>()
        .toJava()
    }
  }

  private val LongLongMap = object : StdDeserializer<Map<Long, Long>>(Map::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Map<Long, Long> {
      val type = ctxt.typeFactory.constructType(ImmutableLongLongMap::class.java)
      return EclipseMapDeserializers.createDeserializer(type).deserialize(p, ctxt).unsafeCast<ImmutableLongLongMap>()
        .toJava()
    }
  }

  private val IntLongMap = object : StdDeserializer<Map<Int, Long>>(Int::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Map<Int, Long> {
      val type = ctxt.typeFactory.constructType(ImmutableIntLongMap::class.java)
      return EclipseMapDeserializers.createDeserializer(type).deserialize(p, ctxt).unsafeCast<ImmutableIntLongMap>()
        .toJava()
    }
  }

  private val LongIntMap = object : StdDeserializer<Map<Long, Int>>(Map::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Map<Long, Int> {
      val type = ctxt.typeFactory.constructType(ImmutableLongIntMap::class.java)
      return EclipseMapDeserializers.createDeserializer(type).deserialize(p, ctxt).unsafeCast<ImmutableLongIntMap>()
        .toJava()
    }
  }
}


