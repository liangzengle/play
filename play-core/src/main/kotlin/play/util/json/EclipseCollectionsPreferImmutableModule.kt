package play.util.json

import com.fasterxml.jackson.core.Version
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.Module
import com.fasterxml.jackson.datatype.eclipsecollections.EclipseCollectionsDeserializers
import com.fasterxml.jackson.datatype.eclipsecollections.PackageVersion
import com.fasterxml.jackson.datatype.eclipsecollections.deser.bag.ImmutableBagDeserializer
import com.fasterxml.jackson.datatype.eclipsecollections.deser.list.ImmutableListDeserializer
import com.fasterxml.jackson.datatype.eclipsecollections.deser.map.EclipseMapDeserializers
import com.fasterxml.jackson.datatype.eclipsecollections.deser.set.ImmutableSetDeserializer
import com.google.auto.service.AutoService
import org.eclipse.collections.api.*
import org.eclipse.collections.api.bag.primitive.*
import org.eclipse.collections.api.list.primitive.*
import org.eclipse.collections.api.map.primitive.*
import org.eclipse.collections.api.set.primitive.*
import play.Log
import play.util.SystemProps
import play.util.reflect.Reflect
import java.util.concurrent.atomic.AtomicBoolean

/**
 * @author LiangZengle
 */
@AutoService(Module::class)
class EclipseCollectionsPreferImmutableModule : Module() {
  //  @JvmStatic
//  fun main(args: Array<String>) {
//    val types = arrayOf("Object", "Boolean", "Byte", "Short", "Char", "Int", "Long", "Float", "Double")
//
//    for (keyType in types) {
//      if (keyType == "Boolean") {
//        continue
//      }
//      for (valueType in types) {
//        if (keyType == valueType && keyType == "Object") {
//          continue
//        }
//        println("put($keyType${valueType}Map::class.java, get(Immutable$keyType${valueType}Map::class.java))")
//      }
//      println()
//    }
//  }

  companion object {
    private val modified = AtomicBoolean()
  }

  override fun version(): Version = PackageVersion.VERSION

  override fun getModuleName(): String = javaClass.simpleName

  override fun setupModule(context: SetupContext) {
    if (modified.compareAndSet(false, true)) {
      modify()
    }
  }

  private fun modify() {
    val preferImmutable = SystemProps.getBoolean("eclipse-collections-deser-prefer-immutable", true)
    Log.info { "Eclipse Collections Deserializers Prefer Immutable: $preferImmutable" }
    if (!preferImmutable) {
      return
    }
    deserializersPreferImmutable()
  }

  private fun deserializersPreferImmutable() {
    val primitiveDeserializersField =
      EclipseCollectionsDeserializers::class.java.getDeclaredField("PRIMITIVE_DESERIALIZERS")
    val primitiveDeserializers =
      Reflect.getFieldValue<MutableMap<Class<out PrimitiveIterable>, JsonDeserializer<*>>>(primitiveDeserializersField, null)!!
    replaceCollectionDeserializer(primitiveDeserializers)

    val entriesField = EclipseMapDeserializers::class.java.getDeclaredField("ENTRIES")
    val entries = Reflect.getFieldValue<MutableMap<Class<*>, Any?>>(entriesField, null)!!
    replaceMapDeserializer(entries)
  }

  private fun replaceMapDeserializer(map: MutableMap<Class<*>, Any?>) {
    map.apply {
      put(ObjectBooleanMap::class.java, get(ImmutableObjectBooleanMap::class.java))
      put(ObjectByteMap::class.java, get(ImmutableObjectByteMap::class.java))
      put(ObjectShortMap::class.java, get(ImmutableObjectShortMap::class.java))
      put(ObjectCharMap::class.java, get(ImmutableObjectCharMap::class.java))
      put(ObjectIntMap::class.java, get(ImmutableObjectIntMap::class.java))
      put(ObjectLongMap::class.java, get(ImmutableObjectLongMap::class.java))
      put(ObjectFloatMap::class.java, get(ImmutableObjectFloatMap::class.java))
      put(ObjectDoubleMap::class.java, get(ImmutableObjectDoubleMap::class.java))

      put(ByteObjectMap::class.java, get(ImmutableByteObjectMap::class.java))
      put(ByteBooleanMap::class.java, get(ImmutableByteBooleanMap::class.java))
      put(ByteByteMap::class.java, get(ImmutableByteByteMap::class.java))
      put(ByteShortMap::class.java, get(ImmutableByteShortMap::class.java))
      put(ByteCharMap::class.java, get(ImmutableByteCharMap::class.java))
      put(ByteIntMap::class.java, get(ImmutableByteIntMap::class.java))
      put(ByteLongMap::class.java, get(ImmutableByteLongMap::class.java))
      put(ByteFloatMap::class.java, get(ImmutableByteFloatMap::class.java))
      put(ByteDoubleMap::class.java, get(ImmutableByteDoubleMap::class.java))

      put(ShortObjectMap::class.java, get(ImmutableShortObjectMap::class.java))
      put(ShortBooleanMap::class.java, get(ImmutableShortBooleanMap::class.java))
      put(ShortByteMap::class.java, get(ImmutableShortByteMap::class.java))
      put(ShortShortMap::class.java, get(ImmutableShortShortMap::class.java))
      put(ShortCharMap::class.java, get(ImmutableShortCharMap::class.java))
      put(ShortIntMap::class.java, get(ImmutableShortIntMap::class.java))
      put(ShortLongMap::class.java, get(ImmutableShortLongMap::class.java))
      put(ShortFloatMap::class.java, get(ImmutableShortFloatMap::class.java))
      put(ShortDoubleMap::class.java, get(ImmutableShortDoubleMap::class.java))

      put(CharObjectMap::class.java, get(ImmutableCharObjectMap::class.java))
      put(CharBooleanMap::class.java, get(ImmutableCharBooleanMap::class.java))
      put(CharByteMap::class.java, get(ImmutableCharByteMap::class.java))
      put(CharShortMap::class.java, get(ImmutableCharShortMap::class.java))
      put(CharCharMap::class.java, get(ImmutableCharCharMap::class.java))
      put(CharIntMap::class.java, get(ImmutableCharIntMap::class.java))
      put(CharLongMap::class.java, get(ImmutableCharLongMap::class.java))
      put(CharFloatMap::class.java, get(ImmutableCharFloatMap::class.java))
      put(CharDoubleMap::class.java, get(ImmutableCharDoubleMap::class.java))

      put(IntObjectMap::class.java, get(ImmutableIntObjectMap::class.java))
      put(IntBooleanMap::class.java, get(ImmutableIntBooleanMap::class.java))
      put(IntByteMap::class.java, get(ImmutableIntByteMap::class.java))
      put(IntShortMap::class.java, get(ImmutableIntShortMap::class.java))
      put(IntCharMap::class.java, get(ImmutableIntCharMap::class.java))
      put(IntIntMap::class.java, get(ImmutableIntIntMap::class.java))
      put(IntLongMap::class.java, get(ImmutableIntLongMap::class.java))
      put(IntFloatMap::class.java, get(ImmutableIntFloatMap::class.java))
      put(IntDoubleMap::class.java, get(ImmutableIntDoubleMap::class.java))

      put(LongObjectMap::class.java, get(ImmutableLongObjectMap::class.java))
      put(LongBooleanMap::class.java, get(ImmutableLongBooleanMap::class.java))
      put(LongByteMap::class.java, get(ImmutableLongByteMap::class.java))
      put(LongShortMap::class.java, get(ImmutableLongShortMap::class.java))
      put(LongCharMap::class.java, get(ImmutableLongCharMap::class.java))
      put(LongIntMap::class.java, get(ImmutableLongIntMap::class.java))
      put(LongLongMap::class.java, get(ImmutableLongLongMap::class.java))
      put(LongFloatMap::class.java, get(ImmutableLongFloatMap::class.java))
      put(LongDoubleMap::class.java, get(ImmutableLongDoubleMap::class.java))

      put(FloatObjectMap::class.java, get(ImmutableFloatObjectMap::class.java))
      put(FloatBooleanMap::class.java, get(ImmutableFloatBooleanMap::class.java))
      put(FloatByteMap::class.java, get(ImmutableFloatByteMap::class.java))
      put(FloatShortMap::class.java, get(ImmutableFloatShortMap::class.java))
      put(FloatCharMap::class.java, get(ImmutableFloatCharMap::class.java))
      put(FloatIntMap::class.java, get(ImmutableFloatIntMap::class.java))
      put(FloatLongMap::class.java, get(ImmutableFloatLongMap::class.java))
      put(FloatFloatMap::class.java, get(ImmutableFloatFloatMap::class.java))
      put(FloatDoubleMap::class.java, get(ImmutableFloatDoubleMap::class.java))

      put(DoubleObjectMap::class.java, get(ImmutableDoubleObjectMap::class.java))
      put(DoubleBooleanMap::class.java, get(ImmutableDoubleBooleanMap::class.java))
      put(DoubleByteMap::class.java, get(ImmutableDoubleByteMap::class.java))
      put(DoubleShortMap::class.java, get(ImmutableDoubleShortMap::class.java))
      put(DoubleCharMap::class.java, get(ImmutableDoubleCharMap::class.java))
      put(DoubleIntMap::class.java, get(ImmutableDoubleIntMap::class.java))
      put(DoubleLongMap::class.java, get(ImmutableDoubleLongMap::class.java))
      put(DoubleFloatMap::class.java, get(ImmutableDoubleFloatMap::class.java))
      put(DoubleDoubleMap::class.java, get(ImmutableDoubleDoubleMap::class.java))
    }
  }

  private fun replaceCollectionDeserializer(primitiveDeserializers: MutableMap<Class<out PrimitiveIterable>, JsonDeserializer<*>>) {
    primitiveDeserializers.apply {
      put(BooleanBag::class.java, ImmutableBagDeserializer.Boolean.INSTANCE)
      put(BooleanIterable::class.java, ImmutableListDeserializer.Boolean.INSTANCE)
      put(BooleanList::class.java, ImmutableListDeserializer.Boolean.INSTANCE)
      put(BooleanSet::class.java, ImmutableSetDeserializer.Boolean.INSTANCE)

      put(ByteBag::class.java, ImmutableBagDeserializer.Byte.INSTANCE)
      put(ByteIterable::class.java, ImmutableListDeserializer.Byte.INSTANCE)
      put(ByteList::class.java, ImmutableListDeserializer.Byte.INSTANCE)
      put(ByteSet::class.java, ImmutableSetDeserializer.Byte.INSTANCE)

      put(ShortBag::class.java, ImmutableBagDeserializer.Short.INSTANCE)
      put(ShortIterable::class.java, ImmutableListDeserializer.Short.INSTANCE)
      put(ShortList::class.java, ImmutableListDeserializer.Short.INSTANCE)
      put(ShortSet::class.java, ImmutableSetDeserializer.Short.INSTANCE)

      put(CharBag::class.java, ImmutableBagDeserializer.Char.INSTANCE)
      put(CharIterable::class.java, ImmutableListDeserializer.Char.INSTANCE)
      put(CharList::class.java, ImmutableListDeserializer.Char.INSTANCE)
      put(CharSet::class.java, ImmutableSetDeserializer.Char.INSTANCE)

      put(IntBag::class.java, ImmutableBagDeserializer.Int.INSTANCE)
      put(IntIterable::class.java, ImmutableListDeserializer.Int.INSTANCE)
      put(IntList::class.java, ImmutableListDeserializer.Int.INSTANCE)
      put(IntSet::class.java, ImmutableSetDeserializer.Int.INSTANCE)

      put(LongBag::class.java, ImmutableBagDeserializer.Long.INSTANCE)
      put(LongIterable::class.java, ImmutableListDeserializer.Long.INSTANCE)
      put(LongList::class.java, ImmutableListDeserializer.Long.INSTANCE)
      put(LongSet::class.java, ImmutableSetDeserializer.Long.INSTANCE)

      put(FloatBag::class.java, ImmutableBagDeserializer.Float.INSTANCE)
      put(FloatIterable::class.java, ImmutableListDeserializer.Float.INSTANCE)
      put(FloatList::class.java, ImmutableListDeserializer.Float.INSTANCE)
      put(FloatSet::class.java, ImmutableSetDeserializer.Float.INSTANCE)

      put(DoubleBag::class.java, ImmutableBagDeserializer.Double.INSTANCE)
      put(DoubleIterable::class.java, ImmutableListDeserializer.Double.INSTANCE)
      put(DoubleList::class.java, ImmutableListDeserializer.Double.INSTANCE)
      put(DoubleSet::class.java, ImmutableSetDeserializer.Double.INSTANCE)
    }
  }
}
