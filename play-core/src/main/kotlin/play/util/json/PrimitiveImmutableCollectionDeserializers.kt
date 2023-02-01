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
import com.google.common.primitives.Ints
import com.google.common.primitives.Longs
import org.eclipse.collections.api.map.primitive.*
import org.eclipse.collections.impl.factory.primitive.IntSets
import org.eclipse.collections.impl.factory.primitive.LongSets
import org.eclipse.collectionx.asJava
import play.util.unsafeCast

object PrimitiveImmutableCollectionDeserializers {

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
      return Ints.asList(*array)
    }
  }

  private val LongArrayList = object : StdDeserializer<List<Long>>(List::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): List<Long> {
      val array =
        PrimitiveArrayDeserializers.forType(Long::class.java).unsafeCast<JsonDeserializer<LongArray>>()
          .deserialize(p, ctxt)
      return Longs.asList(*array)
    }
  }

  private val IntSet = object : StdDeserializer<Set<Int>>(Set::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Set<Int> {
      val array =
        PrimitiveArrayDeserializers.forType(Int::class.java).unsafeCast<JsonDeserializer<IntArray>>()
          .deserialize(p, ctxt)
     return IntSets.immutable.with(*array).asJava()
    }
  }

  private val LongSet = object : StdDeserializer<Set<Long>>(Set::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Set<Long> {
      val array =
        PrimitiveArrayDeserializers.forType(Long::class.java).unsafeCast<JsonDeserializer<LongArray>>()
          .deserialize(p, ctxt)
      return LongSets.immutable.with(*array).asJava()
    }
  }

  private val IntIntHashMap = object : StdDeserializer<Map<Int, Int>>(Map::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Map<Int, Int> {
      val type = ctxt.typeFactory.constructType(ImmutableIntIntMap::class.java)
      return EclipseMapDeserializers.createDeserializer(type).deserialize(p, ctxt).unsafeCast<ImmutableIntIntMap>()
        .asJava()
    }
  }

  private val LongLongHashMap = object : StdDeserializer<Map<Long, Long>>(Map::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Map<Long, Long> {
      val type = ctxt.typeFactory.constructType(ImmutableLongLongMap::class.java)
      return EclipseMapDeserializers.createDeserializer(type).deserialize(p, ctxt).unsafeCast<ImmutableLongLongMap>()
        .asJava()
    }
  }

  private val IntLongHashMap = object : StdDeserializer<Map<Int, Long>>(Map::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Map<Int, Long> {
      val type = ctxt.typeFactory.constructType(ImmutableIntLongMap::class.java)
      return EclipseMapDeserializers.createDeserializer(type).deserialize(p, ctxt).unsafeCast<ImmutableIntLongMap>()
        .asJava()
    }
  }

  private val LongIntHashMap = object : StdDeserializer<Map<Long, Int>>(Map::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Map<Long, Int> {
      val type = ctxt.typeFactory.constructType(ImmutableLongIntMap::class.java)
      return EclipseMapDeserializers.createDeserializer(type).deserialize(p, ctxt).unsafeCast<ImmutableLongIntMap>()
        .asJava()
    }
  }

  private fun intObjectHashMap(objType: JavaType) = object : StdDeserializer<Map<Int, Any>>(Map::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Map<Int, Any> {
      val type = ctxt.typeFactory.constructReferenceType(ImmutableIntObjectMap::class.java, objType)
      return EclipseMapDeserializers.createDeserializer(type).deserialize(p, ctxt)
        .unsafeCast<ImmutableIntObjectMap<Any>>()
        .asJava()
    }
  }

  private fun longObjectHashMap(objType: JavaType) = object : StdDeserializer<Map<Long, Any>>(Map::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Map<Long, Any> {
      val type = ctxt.typeFactory.constructReferenceType(ImmutableLongObjectMap::class.java, objType)
      return EclipseMapDeserializers.createDeserializer(type).deserialize(p, ctxt)
        .unsafeCast<ImmutableLongObjectMap<Any>>()
        .asJava()
    }
  }

  private fun objectIntHashMap(objType: JavaType) = object : StdDeserializer<Map<Any, Int>>(Map::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Map<Any, Int> {
      val type = ctxt.typeFactory.constructReferenceType(ImmutableObjectIntMap::class.java, objType)
      return EclipseMapDeserializers.createDeserializer(type).deserialize(p, ctxt)
        .unsafeCast<ImmutableObjectIntMap<Any>>()
        .asJava()
    }
  }

  private fun objectLongHashMap(objType: JavaType) = object : StdDeserializer<Map<Any, Long>>(Map::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Map<Any, Long> {
      val type = ctxt.typeFactory.constructReferenceType(ImmutableObjectLongMap::class.java, objType)
      return EclipseMapDeserializers.createDeserializer(type).deserialize(p, ctxt)
        .unsafeCast<ImmutableObjectLongMap<Any>>()
        .asJava()
    }
  }
}


