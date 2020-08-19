package play.util.collection.deser

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.Version
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.deser.Deserializers
import com.fasterxml.jackson.databind.deser.std.PrimitiveArrayDeserializers
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.datatype.eclipsecollections.PackageVersion
import com.google.auto.service.AutoService
import play.util.collection.BitSet
import play.util.collection.MutableEnumIntMap
import play.util.collection.MutableEnumLongMap
import play.util.unsafeCast

/**
 *
 * @author LiangZengle
 */
@AutoService(Module::class)
class PlayCollectionModule : Module() {
  override fun version(): Version {
    return PackageVersion.VERSION
  }

  override fun getModuleName(): String {
    return javaClass.simpleName
  }

  override fun setupModule(context: SetupContext) {
    context.addDeserializers(PlayCollectionDeserializers())
  }
}

internal class PlayCollectionDeserializers : Deserializers.Base() {
  override fun findBeanDeserializer(
    type: JavaType, config: DeserializationConfig?, beanDesc: BeanDescription?
  ): JsonDeserializer<*>? {
    if (type.rawClass == BitSet::class.java) {
      return BitSetDeserializer()
    }
    if (type.rawClass == MutableEnumIntMap::class.java) {
      return MutableEnumIntMapDeserializer(type.bindings.typeParameters[0].rawClass)
    }
    if (type.rawClass == MutableEnumLongMap::class.java) {
      return MutableEnumLongMapDeserializer(type.bindings.typeParameters[0].rawClass)
    }
    return null
  }
}

internal class BitSetDeserializer : StdDeserializer<BitSet>(BitSet::class.java) {
  override fun deserialize(p: JsonParser, ctxt: DeserializationContext): BitSet {
    val array = PrimitiveArrayDeserializers.forType(Long::class.java).unsafeCast<JsonDeserializer<LongArray>>()
      .deserialize(p, ctxt)
    return BitSet(array)
  }
}

internal class MutableEnumIntMapDeserializer(private val keyType: Class<*>) :
  StdDeserializer<MutableEnumIntMap<*>>(MutableEnumIntMap::class.java) {
  override fun deserialize(p: JsonParser, ctxt: DeserializationContext): MutableEnumIntMap<*> {
    val array =
      PrimitiveArrayDeserializers.forType(Int::class.java).unsafeCast<JsonDeserializer<IntArray>>().deserialize(p, ctxt)
    return MutableEnumIntMap(keyType.unsafeCast<Class<out Enum<*>>>(), array)
  }
}

internal class MutableEnumLongMapDeserializer(private val keyType: Class<*>) :
  StdDeserializer<MutableEnumLongMap<*>>(MutableEnumLongMap::class.java) {
  override fun deserialize(p: JsonParser, ctxt: DeserializationContext): MutableEnumLongMap<*> {
    val array = PrimitiveArrayDeserializers.forType(Long::class.java).unsafeCast<JsonDeserializer<LongArray>>()
      .deserialize(p, ctxt)
    return MutableEnumLongMap(keyType.unsafeCast<Class<out Enum<*>>>(), array)
  }
}

