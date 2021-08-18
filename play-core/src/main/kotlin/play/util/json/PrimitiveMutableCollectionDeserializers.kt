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
import it.unimi.dsi.fastutil.ints.*
import it.unimi.dsi.fastutil.longs.*
import it.unimi.dsi.fastutil.objects.*
import org.eclipse.collections.api.map.primitive.*
import play.util.unsafeCast
import java.util.*

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
    if (SortedSet::class.java == collectionType) {
      return forSortedSet(primitiveContentType)
    }
    return null
  }

  fun forMapType(type: MapType): JsonDeserializer<*>? {
    val mapType = type.rawClass
    if (Map::class.java == mapType) {
      return forMap(type)
    }
    if (SortedMap::class.java == mapType) {
      return forSortedMap(type)
    }
    return null
  }

  private fun forSortedMap(type: MapType): JsonDeserializer<*>? {
    val primitiveKeyType = type.keyType.rawClass.kotlin.javaPrimitiveType
    val primitiveValueType = type.contentType.rawClass.kotlin.javaPrimitiveType
    if (primitiveKeyType == null && primitiveValueType == null) {
      return null
    }
    if (primitiveKeyType == Int::class.java) {
      return when (primitiveValueType) {
        null -> intObjectSortMap(type.contentType)
        Int::class.java -> IntIntSortedMap
        Long::class.java -> IntLongSortedMap
        else -> null
      }
    }
    if (primitiveKeyType == Long::class.java) {
      return when (primitiveValueType) {
        null -> longObjectSortedMap(type.contentType)
        Int::class.java -> LongIntSortedMap
        Long::class.java -> LongLongSortedMap
        else -> null
      }
    }
    if (primitiveValueType == Int::class.java && primitiveKeyType == null) {
      return objectIntSortedMap(type.contentType)
    }
    if (primitiveValueType == Long::class.java && primitiveKeyType == null) {
      return objectLongSortedMap(type.contentType)
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

  private fun forSortedSet(primitiveContentType: Class<*>): JsonDeserializer<*>? {
    return when (primitiveContentType) {
      Int::class.java -> IntSortedSet
      Long::class.java -> LongSortedSet
      else -> null
    }
  }

  private val IntArrayList = object : StdDeserializer<List<Int>>(List::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): List<Int> {
      val array =
        PrimitiveArrayDeserializers.forType(Int::class.java).unsafeCast<JsonDeserializer<IntArray>>()
          .deserialize(p, ctxt)
      return IntArrayList(array)
    }
  }

  private val LongArrayList = object : StdDeserializer<List<Long>>(List::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): List<Long> {
      val array =
        PrimitiveArrayDeserializers.forType(Long::class.java).unsafeCast<JsonDeserializer<LongArray>>()
          .deserialize(p, ctxt)
      return LongArrayList(array)
    }
  }

  private val IntSet = object : StdDeserializer<Set<Int>>(Set::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Set<Int> {
      val array =
        PrimitiveArrayDeserializers.forType(Int::class.java).unsafeCast<JsonDeserializer<IntArray>>()
          .deserialize(p, ctxt)
      if (array.size > 8) {
        return IntOpenHashSet.of(*array)
      }
      return IntArraySet(array)
    }
  }

  private val LongSet = object : StdDeserializer<Set<Long>>(Set::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Set<Long> {
      val array =
        PrimitiveArrayDeserializers.forType(Long::class.java).unsafeCast<JsonDeserializer<LongArray>>()
          .deserialize(p, ctxt)
      if (array.size > 8) {
        return LongOpenHashSet(array)
      }
      return LongArraySet(array)
    }
  }

  private val IntSortedSet = object : StdDeserializer<SortedSet<Int>>(SortedSet::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): SortedSet<Int> {
      val array =
        PrimitiveArrayDeserializers.forType(Int::class.java).unsafeCast<JsonDeserializer<IntArray>>()
          .deserialize(p, ctxt)
      val set = IntRBTreeSet()
      for (elem in array) {
        set.add(elem)
      }
      return set
    }
  }

  private val LongSortedSet = object : StdDeserializer<SortedSet<Long>>(SortedSet::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): SortedSet<Long> {
      val array =
        PrimitiveArrayDeserializers.forType(Long::class.java).unsafeCast<JsonDeserializer<LongArray>>()
          .deserialize(p, ctxt)
      val set = LongRBTreeSet()
      for (elem in array) {
        set.add(elem)
      }
      return set
    }
  }

  private val IntIntHashMap = object : StdDeserializer<Map<Int, Int>>(Map::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Map<Int, Int> {
      val type = ctxt.typeFactory.constructType(IntIntMap::class.java)
      val map = EclipseMapDeserializers.createDeserializer(type).deserialize(p, ctxt).unsafeCast<IntIntMap>()
      val result = if (map.size() > 8) Int2IntOpenHashMap(map.size()) else Int2IntArrayMap(map.size())
      for (pair in map.keyValuesView()) {
        result.put(pair.one, pair.two)
      }
      return result
    }
  }

  private val LongLongHashMap = object : StdDeserializer<Map<Long, Long>>(Map::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Map<Long, Long> {
      val type = ctxt.typeFactory.constructType(LongLongMap::class.java)
      val map = EclipseMapDeserializers.createDeserializer(type).deserialize(p, ctxt).unsafeCast<LongLongMap>()
      val result = if (map.size() > 8) Long2LongOpenHashMap(map.size()) else Long2LongArrayMap(map.size())
      for (pair in map.keyValuesView()) {
        result.put(pair.one, pair.two)
      }
      return result
    }
  }

  private val IntLongHashMap = object : StdDeserializer<Map<Int, Long>>(Map::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Map<Int, Long> {
      val type = ctxt.typeFactory.constructType(IntLongMap::class.java)
      val map = EclipseMapDeserializers.createDeserializer(type).deserialize(p, ctxt).unsafeCast<IntLongMap>()
      val result = if (map.size() > 8) Int2LongOpenHashMap(map.size()) else Int2LongArrayMap(map.size())
      for (pair in map.keyValuesView()) {
        result.put(pair.one, pair.two)
      }
      return result
    }
  }

  private val LongIntHashMap = object : StdDeserializer<Map<Long, Int>>(Map::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Map<Long, Int> {
      val type = ctxt.typeFactory.constructType(LongIntMap::class.java)
      val map = EclipseMapDeserializers.createDeserializer(type).deserialize(p, ctxt).unsafeCast<LongIntMap>()
      val result = if (map.size() > 8) Long2IntOpenHashMap(map.size()) else Long2IntArrayMap(map.size())
      for (pair in map.keyValuesView()) {
        result.put(pair.one, pair.two)
      }
      return result
    }
  }

  private fun intObjectHashMap(objType: JavaType) = object : StdDeserializer<Map<Int, Any>>(Map::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Map<Int, Any> {
      val type = ctxt.typeFactory.constructReferenceType(IntObjectMap::class.java, objType)
      val map = EclipseMapDeserializers.createDeserializer(type).deserialize(p, ctxt)
        .unsafeCast<IntObjectMap<Any>>()
      val result = if (map.size() > 8) Int2ObjectOpenHashMap(map.size()) else Int2ObjectArrayMap<Any>(map.size())
      for (pair in map.keyValuesView()) {
        result.put(pair.one, pair.two)
      }
      return result
    }
  }

  private fun longObjectHashMap(objType: JavaType) = object : StdDeserializer<Map<Long, Any>>(Map::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Map<Long, Any> {
      val type = ctxt.typeFactory.constructReferenceType(LongObjectMap::class.java, objType)
      val map = EclipseMapDeserializers.createDeserializer(type).deserialize(p, ctxt)
        .unsafeCast<LongObjectMap<Any>>()
      val result = if (map.size() > 8) Long2ObjectOpenHashMap(map.size()) else Long2ObjectArrayMap<Any>(map.size())
      for (pair in map.keyValuesView()) {
        result.put(pair.one, pair.two)
      }
      return result
    }
  }

  private fun objectIntHashMap(objType: JavaType) = object : StdDeserializer<Map<Any, Int>>(Map::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Map<Any, Int> {
      val type = ctxt.typeFactory.constructReferenceType(ObjectIntMap::class.java, objType)
      val map = EclipseMapDeserializers.createDeserializer(type).deserialize(p, ctxt)
        .unsafeCast<ObjectIntMap<Any>>()
      val result = if (map.size() > 8) Object2IntOpenHashMap(map.size()) else Object2IntArrayMap<Any>(map.size())
      for (pair in map.keyValuesView()) {
        result.put(pair.one, pair.two)
      }
      return result
    }
  }

  private fun objectLongHashMap(objType: JavaType) = object : StdDeserializer<Map<Any, Long>>(Map::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Map<Any, Long> {
      val type = ctxt.typeFactory.constructReferenceType(ObjectLongMap::class.java, objType)
      val map = EclipseMapDeserializers.createDeserializer(type).deserialize(p, ctxt)
        .unsafeCast<ObjectLongMap<Any>>()
      val result = if (map.size() > 8) Object2LongOpenHashMap(map.size()) else Object2LongArrayMap<Any>(map.size())
      for (pair in map.keyValuesView()) {
        result.put(pair.one, pair.two)
      }
      return result
    }
  }

  private val IntIntSortedMap = object : StdDeserializer<SortedMap<Int, Int>>(SortedMap::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): SortedMap<Int, Int> {
      val type = ctxt.typeFactory.constructType(IntIntMap::class.java)
      val map = EclipseMapDeserializers.createDeserializer(type).deserialize(p, ctxt).unsafeCast<IntIntMap>()
      val sortedMap = Int2IntRBTreeMap()
      for (pair in map.keyValuesView()) {
        sortedMap[pair.one] = pair.two
      }
      return sortedMap
    }
  }

  private val LongLongSortedMap = object : StdDeserializer<SortedMap<Long, Long>>(SortedMap::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): SortedMap<Long, Long> {
      val type = ctxt.typeFactory.constructType(LongLongMap::class.java)
      val map = EclipseMapDeserializers.createDeserializer(type).deserialize(p, ctxt).unsafeCast<LongLongMap>()
      val sortedMap = Long2LongRBTreeMap()
      for (pair in map.keyValuesView()) {
        sortedMap[pair.one] = pair.two
      }
      return sortedMap
    }
  }

  private val IntLongSortedMap = object : StdDeserializer<SortedMap<Int, Long>>(SortedMap::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): SortedMap<Int, Long> {
      val type = ctxt.typeFactory.constructType(IntLongMap::class.java)
      val map = EclipseMapDeserializers.createDeserializer(type).deserialize(p, ctxt).unsafeCast<IntLongMap>()
      val sortedMap = Int2LongRBTreeMap()
      for (pair in map.keyValuesView()) {
        sortedMap[pair.one] = pair.two
      }
      return sortedMap
    }
  }

  private val LongIntSortedMap = object : StdDeserializer<SortedMap<Long, Int>>(SortedMap::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): SortedMap<Long, Int> {
      val type = ctxt.typeFactory.constructType(ImmutableLongIntMap::class.java)
      val map = EclipseMapDeserializers.createDeserializer(type).deserialize(p, ctxt).unsafeCast<ImmutableLongIntMap>()
      val sortedMap = Long2IntRBTreeMap()
      for (pair in map.keyValuesView()) {
        sortedMap[pair.one] = pair.two
      }
      return Collections.unmodifiableSortedMap(sortedMap)
    }
  }

  private fun intObjectSortMap(objType: JavaType) = object : StdDeserializer<SortedMap<Int, Any>>(Int::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): SortedMap<Int, Any> {
      val type = ctxt.typeFactory.constructReferenceType(IntObjectMap::class.java, objType)
      val map = EclipseMapDeserializers.createDeserializer(type).deserialize(p, ctxt)
        .unsafeCast<IntObjectMap<Any>>()
      val sortedMap = Int2ObjectRBTreeMap<Any>()
      for (pair in map.keyValuesView()) {
        sortedMap[pair.one] = pair.two
      }
      return sortedMap
    }
  }

  private fun longObjectSortedMap(objType: JavaType) = object : StdDeserializer<SortedMap<Long, Any>>(Map::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): SortedMap<Long, Any> {
      val type = ctxt.typeFactory.constructReferenceType(LongObjectMap::class.java, objType)
      val map = EclipseMapDeserializers.createDeserializer(type).deserialize(p, ctxt)
        .unsafeCast<LongObjectMap<Any>>()
      val sortedMap = Long2ObjectRBTreeMap<Any>()
      for (pair in map.keyValuesView()) {
        sortedMap[pair.one] = pair.two
      }
      return sortedMap
    }
  }

  private fun objectIntSortedMap(objType: JavaType) =
    object : StdDeserializer<SortedMap<Any, Int>>(SortedMap::class.java) {
      override fun deserialize(p: JsonParser, ctxt: DeserializationContext): SortedMap<Any, Int> {
        val type = ctxt.typeFactory.constructReferenceType(ObjectIntMap::class.java, objType)
        val map = EclipseMapDeserializers.createDeserializer(type).deserialize(p, ctxt)
          .unsafeCast<ObjectIntMap<Any>>()
        val sortedMap = Object2IntRBTreeMap<Any>()
        for (pair in map.keyValuesView()) {
          sortedMap[pair.one] = pair.two
        }
        return sortedMap
      }
    }

  private fun objectLongSortedMap(objType: JavaType) =
    object : StdDeserializer<SortedMap<Any, Long>>(SortedMap::class.java) {
      override fun deserialize(p: JsonParser, ctxt: DeserializationContext): SortedMap<Any, Long> {
        val type = ctxt.typeFactory.constructReferenceType(ObjectLongMap::class.java, objType)
        val map = EclipseMapDeserializers.createDeserializer(type).deserialize(p, ctxt)
          .unsafeCast<ObjectLongMap<Any>>()
        val sortedMap = Object2LongRBTreeMap<Any>()
        for (pair in map.keyValuesView()) {
          sortedMap[pair.one] = pair.two
        }
        return sortedMap
      }
    }
}


