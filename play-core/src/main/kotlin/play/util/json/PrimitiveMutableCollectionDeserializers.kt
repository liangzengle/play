package play.util.json

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.deser.std.PrimitiveArrayDeserializers
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.type.CollectionType
import com.fasterxml.jackson.databind.type.MapType
import com.fasterxml.jackson.datatype.eclipsecollections.deser.map.EclipseMapDeserializers
import org.eclipse.collections.api.map.primitive.*
import org.eclipse.collections.impl.factory.primitive.*
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList
import org.eclipse.collections.impl.list.mutable.primitive.LongArrayList
import org.eclipse.collectionx.toJava
import play.util.unsafeCast

object PrimitiveMutableCollectionDeserializers {

  fun forCollectionType(type: CollectionType): JsonDeserializer<*>? {
    val primitiveContentType = type.contentType.rawClass.kotlin.javaPrimitiveType ?: return null
    val collectionType = type.rawClass
    if (List::class.java == collectionType) {
      return forList(primitiveContentType)
    }
    if (Set::class.java == collectionType) {
      return forSet(primitiveContentType)
    }
    if (Collection::class.java == collectionType) {
      return forList(primitiveContentType)
    }
    return null
  }

  fun forMapType(type: MapType): JsonDeserializer<*>? {
    val mapType = type.rawClass
    if (Map::class.java == mapType) {
      return forMap(type)
    }
    return null
  }

  private fun forMap(type: MapType): JsonDeserializer<*>? {
    val primitiveKeyType = type.keyType.rawClass.kotlin.javaPrimitiveType
    val primitiveValueType = type.contentType.rawClass.kotlin.javaPrimitiveType
    if (primitiveKeyType == null && primitiveValueType == null) {
      return null
    }
    if (primitiveKeyType == Int::class.java) {
      return when (primitiveValueType) {
        null -> intObjectHashMap(type.contentType)
        Int::class.java -> IntIntHashMap
        Long::class.java -> IntLongHashMap
        else -> null
      }
    }
    if (primitiveKeyType == Long::class.java) {
      return when (primitiveValueType) {
        null -> longObjectHashMap(type.contentType)
        Int::class.java -> LongIntHashMap
        Long::class.java -> LongLongHashMap
        else -> null
      }
    }
    if (primitiveValueType == Int::class.java && primitiveKeyType == null) {
      return objectIntHashMap(type.contentType)
    }
    if (primitiveValueType == Long::class.java && primitiveKeyType == null) {
      return objectLongHashMap(type.contentType)
    }
    return null
  }

  private fun forList(primitiveContentType: Class<*>): JsonDeserializer<*>? {
    return when (primitiveContentType) {
      Int::class.java -> IntArrayList
      Long::class.java -> LongArrayList
      else -> null
    }
  }

  private fun forSet(primitiveContentType: Class<*>): JsonDeserializer<*>? {
    return when (primitiveContentType) {
      Int::class.java -> IntSet
      Long::class.java -> LongSet
      else -> null
    }
  }

  private val IntArrayList = object : StdDeserializer<List<Int>>(List::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): List<Int> {
      val array =
        PrimitiveArrayDeserializers.forType(Int::class.java).unsafeCast<JsonDeserializer<IntArray>>()
          .deserialize(p, ctxt)
      return IntArrayList(*array).toJava()
    }
  }

  private val LongArrayList = object : StdDeserializer<List<Long>>(List::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): List<Long> {
      val array =
        PrimitiveArrayDeserializers.forType(Long::class.java).unsafeCast<JsonDeserializer<LongArray>>()
          .deserialize(p, ctxt)
      return LongArrayList(*array).toJava()
    }
  }

  private val IntSet = object : StdDeserializer<Set<Int>>(Set::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Set<Int> {
      val array =
        PrimitiveArrayDeserializers.forType(Int::class.java).unsafeCast<JsonDeserializer<IntArray>>()
          .deserialize(p, ctxt)
      return IntSets.mutable.of(*array).toJava()
    }
  }

  private val LongSet = object : StdDeserializer<Set<Long>>(Set::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Set<Long> {
      val array =
        PrimitiveArrayDeserializers.forType(Long::class.java).unsafeCast<JsonDeserializer<LongArray>>()
          .deserialize(p, ctxt)
      return LongSets.mutable.of(*array).toJava()
    }
  }

  private val IntIntHashMap = object : StdDeserializer<Map<Int, Int>>(Map::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Map<Int, Int> {
      val type = ctxt.typeFactory.constructType(IntIntMap::class.java)
      val map = EclipseMapDeserializers.createDeserializer(type).deserialize(p, ctxt).unsafeCast<IntIntMap>()
      val result = IntIntMaps.mutable.ofInitialCapacity(map.size())
      for (pair in map.keyValuesView()) {
        result.put(pair.one, pair.two)
      }
      return result.toJava()
    }
  }

  private val LongLongHashMap = object : StdDeserializer<Map<Long, Long>>(Map::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Map<Long, Long> {
      val type = ctxt.typeFactory.constructType(LongLongMap::class.java)
      val map = EclipseMapDeserializers.createDeserializer(type).deserialize(p, ctxt).unsafeCast<LongLongMap>()
      val result = LongLongMaps.mutable.ofInitialCapacity(map.size())
      for (pair in map.keyValuesView()) {
        result.put(pair.one, pair.two)
      }
      return result.toJava()
    }
  }

  private val IntLongHashMap = object : StdDeserializer<Map<Int, Long>>(Map::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Map<Int, Long> {
      val type = ctxt.typeFactory.constructType(IntLongMap::class.java)
      val map = EclipseMapDeserializers.createDeserializer(type).deserialize(p, ctxt).unsafeCast<IntLongMap>()
      val result = IntLongMaps.mutable.ofInitialCapacity(map.size())
      for (pair in map.keyValuesView()) {
        result.put(pair.one, pair.two)
      }
      return result.toJava()
    }
  }

  private val LongIntHashMap = object : StdDeserializer<Map<Long, Int>>(Map::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Map<Long, Int> {
      val type = ctxt.typeFactory.constructType(LongIntMap::class.java)
      val map = EclipseMapDeserializers.createDeserializer(type).deserialize(p, ctxt).unsafeCast<LongIntMap>()
      val result = LongIntMaps.mutable.ofInitialCapacity(map.size())
      for (pair in map.keyValuesView()) {
        result.put(pair.one, pair.two)
      }
      return result.toJava()
    }
  }

  private fun intObjectHashMap(objType: JavaType) = object : StdDeserializer<Map<Int, Any>>(Map::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Map<Int, Any> {
      val type = ctxt.typeFactory.constructReferenceType(IntObjectMap::class.java, objType)
      val map = EclipseMapDeserializers.createDeserializer(type).deserialize(p, ctxt)
        .unsafeCast<IntObjectMap<Any>>()
      val result = IntObjectMaps.mutable.ofInitialCapacity<Any>(map.size())
      for (pair in map.keyValuesView()) {
        result.put(pair.one, pair.two)
      }
      return result.toJava()
    }
  }

  private fun longObjectHashMap(objType: JavaType) = object : StdDeserializer<Map<Long, Any>>(Map::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Map<Long, Any> {
      val type = ctxt.typeFactory.constructReferenceType(LongObjectMap::class.java, objType)
      val map = EclipseMapDeserializers.createDeserializer(type).deserialize(p, ctxt)
        .unsafeCast<LongObjectMap<Any>>()
      val result = LongObjectMaps.mutable.ofInitialCapacity<Any>(map.size())
      for (pair in map.keyValuesView()) {
        result.put(pair.one, pair.two)
      }
      return result.toJava()
    }
  }

  private fun objectIntHashMap(objType: JavaType) = object : StdDeserializer<Map<Any, Int>>(Map::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Map<Any, Int> {
      val type = ctxt.typeFactory.constructReferenceType(ObjectIntMap::class.java, objType)
      val map = EclipseMapDeserializers.createDeserializer(type).deserialize(p, ctxt)
        .unsafeCast<ObjectIntMap<Any>>()
      val result = ObjectIntMaps.mutable.ofInitialCapacity<Any>(map.size())
      for (pair in map.keyValuesView()) {
        result.put(pair.one, pair.two)
      }
      return result.toJava()
    }
  }

  private fun objectLongHashMap(objType: JavaType) = object : StdDeserializer<Map<Any, Long>>(Map::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Map<Any, Long> {
      val type = ctxt.typeFactory.constructReferenceType(ObjectLongMap::class.java, objType)
      val map = EclipseMapDeserializers.createDeserializer(type).deserialize(p, ctxt)
        .unsafeCast<ObjectLongMap<Any>>()
      val result = ObjectLongMaps.mutable.ofInitialCapacity<Any>(map.size())
      for (pair in map.keyValuesView()) {
        result.put(pair.one, pair.two)
      }
      return result.toJava()
    }
  }
}


