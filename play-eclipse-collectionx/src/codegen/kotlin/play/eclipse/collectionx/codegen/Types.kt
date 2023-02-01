package play.eclipse.collectionx.codegen

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.eclipse.collections.api.collection.primitive.*
import org.eclipse.collections.api.factory.list.primitive.*
import org.eclipse.collections.api.factory.map.primitive.*
import org.eclipse.collections.api.iterator.*
import org.eclipse.collections.api.iterator.BooleanIterator
import org.eclipse.collections.api.iterator.ByteIterator
import org.eclipse.collections.api.iterator.CharIterator
import org.eclipse.collections.api.iterator.DoubleIterator
import org.eclipse.collections.api.iterator.FloatIterator
import org.eclipse.collections.api.iterator.IntIterator
import org.eclipse.collections.api.iterator.LongIterator
import org.eclipse.collections.api.iterator.ShortIterator
import org.eclipse.collections.api.list.primitive.*
import org.eclipse.collections.api.map.primitive.*
import org.eclipse.collections.api.set.primitive.*
import org.eclipse.collections.api.tuple.primitive.*
import org.eclipse.collections.impl.collection.mutable.UnmodifiableMutableCollection
import org.eclipse.collections.impl.collection.mutable.primitive.*
import org.eclipse.collections.impl.set.mutable.UnmodifiableMutableSet
import org.eclipse.collections.impl.set.mutable.primitive.*
import kotlin.reflect.KClass

/**
 *
 * @author LiangZengle
 */
object Types {
  val newListWith = MemberName("org.eclipse.collections.impl.list.immutable.primitive", "newListWith")

  val primitiveTypes = listOf(
    Boolean::class,
    Byte::class,
    Short::class,
    Char::class,
    Int::class,
    Long::class,
    Float::class,
    Double::class
  )

  val iteratorTypes = listOf(
    Boolean::class to BooleanIterator::class,
    Byte::class to ByteIterator::class,
    Short::class to ShortIterator::class,
    Char::class to CharIterator::class,
    Int::class to IntIterator::class,
    Long::class to LongIterator::class,
    Float::class to FloatIterator::class,
    Double::class to DoubleIterator::class
  )

  val immutableCollectionTypes = listOf(
    Boolean::class to ImmutableBooleanCollection::class,
    Byte::class to ImmutableByteCollection::class,
    Short::class to ImmutableShortCollection::class,
    Char::class to ImmutableCharCollection::class,
    Int::class to ImmutableIntCollection::class,
    Long::class to ImmutableLongCollection::class,
    Float::class to ImmutableFloatCollection::class,
    Double::class to ImmutableDoubleCollection::class,
  )

  val mutableCollectionTypes = listOf(
    Boolean::class to MutableBooleanCollection::class,
    Byte::class to MutableByteCollection::class,
    Short::class to MutableShortCollection::class,
    Char::class to MutableCharCollection::class,
    Int::class to MutableIntCollection::class,
    Long::class to MutableLongCollection::class,
    Float::class to MutableFloatCollection::class,
    Double::class to MutableDoubleCollection::class,
  )

  val listTypes = listOf(
    Boolean::class to BooleanList::class,
    Byte::class to ByteList::class,
    Short::class to ShortList::class,
    Char::class to CharList::class,
    Int::class to IntList::class,
    Long::class to LongList::class,
    Float::class to FloatList::class,
    Double::class to DoubleList::class,
  )

  val immutableListTypes = listOf(
    Boolean::class to ImmutableBooleanList::class,
    Byte::class to ImmutableByteList::class,
    Short::class to ImmutableShortList::class,
    Char::class to ImmutableCharList::class,
    Int::class to ImmutableIntList::class,
    Long::class to ImmutableLongList::class,
    Float::class to ImmutableFloatList::class,
    Double::class to ImmutableDoubleList::class,
  )

  val mutableListTypes = listOf(
    Boolean::class to MutableBooleanList::class,
    Byte::class to MutableByteList::class,
    Short::class to MutableShortList::class,
    Char::class to MutableCharList::class,
    Int::class to MutableIntList::class,
    Long::class to MutableLongList::class,
    Float::class to MutableFloatList::class,
    Double::class to MutableDoubleList::class,
  )

  val setTypes = listOf(
    Boolean::class to BooleanSet::class,
    Byte::class to ByteSet::class,
    Short::class to ShortSet::class,
    Char::class to CharSet::class,
    Int::class to IntSet::class,
    Long::class to LongSet::class
  )

  val immutableSetTypes = listOf(
    Boolean::class to ImmutableBooleanSet::class,
    Byte::class to ImmutableByteSet::class,
    Short::class to ImmutableShortSet::class,
    Char::class to ImmutableCharSet::class,
    Int::class to ImmutableIntSet::class,
    Long::class to ImmutableLongSet::class
  )

  val mutableSetTypes = listOf(
    Boolean::class to MutableBooleanSet::class,
    Byte::class to MutableByteSet::class,
    Short::class to MutableShortSet::class,
    Char::class to MutableCharSet::class,
    Int::class to MutableIntSet::class,
    Long::class to MutableLongSet::class
  )

  val mapTypes = listOf(
    Triple(Byte::class, Boolean::class, ByteBooleanMap::class),
    Triple(Byte::class, Byte::class, ByteByteMap::class),
    Triple(Byte::class, Short::class, ByteShortMap::class),
    Triple(Byte::class, Char::class, ByteCharMap::class),
    Triple(Byte::class, Int::class, ByteIntMap::class),
    Triple(Byte::class, Long::class, ByteLongMap::class),
    Triple(Byte::class, Float::class, ByteFloatMap::class),
    Triple(Byte::class, Double::class, ByteDoubleMap::class),
    Triple(Byte::class, Object::class, ByteObjectMap::class),
    Triple(Short::class, Boolean::class, ShortBooleanMap::class),
    Triple(Short::class, Byte::class, ShortByteMap::class),
    Triple(Short::class, Short::class, ShortShortMap::class),
    Triple(Short::class, Char::class, ShortCharMap::class),
    Triple(Short::class, Int::class, ShortIntMap::class),
    Triple(Short::class, Long::class, ShortLongMap::class),
    Triple(Short::class, Float::class, ShortFloatMap::class),
    Triple(Short::class, Double::class, ShortDoubleMap::class),
    Triple(Short::class, Object::class, ShortObjectMap::class),
    Triple(Char::class, Boolean::class, CharBooleanMap::class),
    Triple(Char::class, Byte::class, CharByteMap::class),
    Triple(Char::class, Short::class, CharShortMap::class),
    Triple(Char::class, Char::class, CharCharMap::class),
    Triple(Char::class, Int::class, CharIntMap::class),
    Triple(Char::class, Long::class, CharLongMap::class),
    Triple(Char::class, Float::class, CharFloatMap::class),
    Triple(Char::class, Double::class, CharDoubleMap::class),
    Triple(Char::class, Object::class, CharObjectMap::class),
    Triple(Int::class, Boolean::class, IntBooleanMap::class),
    Triple(Int::class, Byte::class, IntByteMap::class),
    Triple(Int::class, Short::class, IntShortMap::class),
    Triple(Int::class, Char::class, IntCharMap::class),
    Triple(Int::class, Int::class, IntIntMap::class),
    Triple(Int::class, Long::class, IntLongMap::class),
    Triple(Int::class, Float::class, IntFloatMap::class),
    Triple(Int::class, Double::class, IntDoubleMap::class),
    Triple(Int::class, Object::class, IntObjectMap::class),
    Triple(Long::class, Boolean::class, LongBooleanMap::class),
    Triple(Long::class, Byte::class, LongByteMap::class),
    Triple(Long::class, Short::class, LongShortMap::class),
    Triple(Long::class, Char::class, LongCharMap::class),
    Triple(Long::class, Int::class, LongIntMap::class),
    Triple(Long::class, Long::class, LongLongMap::class),
    Triple(Long::class, Float::class, LongFloatMap::class),
    Triple(Long::class, Double::class, LongDoubleMap::class),
    Triple(Long::class, Object::class, LongObjectMap::class),
//    Triple(Float::class, Boolean::class, FloatBooleanMap::class),
//    Triple(Float::class, Byte::class, FloatByteMap::class),
//    Triple(Float::class, Short::class, FloatShortMap::class),
//    Triple(Float::class, Char::class, FloatCharMap::class),
//    Triple(Float::class, Int::class, FloatIntMap::class),
//    Triple(Float::class, Long::class, FloatLongMap::class),
//    Triple(Float::class, Float::class, FloatFloatMap::class),
//    Triple(Float::class, Double::class, FloatDoubleMap::class),
//    Triple(Float::class, Object::class, FloatObjectMap::class),
//    Triple(Double::class, Boolean::class, DoubleBooleanMap::class),
//    Triple(Double::class, Byte::class, DoubleByteMap::class),
//    Triple(Double::class, Short::class, DoubleShortMap::class),
//    Triple(Double::class, Char::class, DoubleCharMap::class),
//    Triple(Double::class, Int::class, DoubleIntMap::class),
//    Triple(Double::class, Long::class, DoubleLongMap::class),
//    Triple(Double::class, Float::class, DoubleFloatMap::class),
//    Triple(Double::class, Double::class, DoubleDoubleMap::class),
//    Triple(Double::class, Object::class, DoubleObjectMap::class),
    Triple(Object::class, Boolean::class, ObjectBooleanMap::class),
    Triple(Object::class, Byte::class, ObjectByteMap::class),
    Triple(Object::class, Short::class, ObjectShortMap::class),
    Triple(Object::class, Char::class, ObjectCharMap::class),
    Triple(Object::class, Int::class, ObjectIntMap::class),
    Triple(Object::class, Long::class, ObjectLongMap::class),
    Triple(Object::class, Float::class, ObjectFloatMap::class),
    Triple(Object::class, Double::class, ObjectDoubleMap::class)
  )

  val immutableMapTypes = listOf(
    Triple(Byte::class, Boolean::class, ImmutableByteBooleanMap::class),
    Triple(Byte::class, Byte::class, ImmutableByteByteMap::class),
    Triple(Byte::class, Short::class, ImmutableByteShortMap::class),
    Triple(Byte::class, Char::class, ImmutableByteCharMap::class),
    Triple(Byte::class, Int::class, ImmutableByteIntMap::class),
    Triple(Byte::class, Long::class, ImmutableByteLongMap::class),
    Triple(Byte::class, Float::class, ImmutableByteFloatMap::class),
    Triple(Byte::class, Double::class, ImmutableByteDoubleMap::class),
    Triple(Byte::class, Object::class, ImmutableByteObjectMap::class),
    Triple(Short::class, Boolean::class, ImmutableShortBooleanMap::class),
    Triple(Short::class, Byte::class, ImmutableShortByteMap::class),
    Triple(Short::class, Short::class, ImmutableShortShortMap::class),
    Triple(Short::class, Char::class, ImmutableShortCharMap::class),
    Triple(Short::class, Int::class, ImmutableShortIntMap::class),
    Triple(Short::class, Long::class, ImmutableShortLongMap::class),
    Triple(Short::class, Float::class, ImmutableShortFloatMap::class),
    Triple(Short::class, Double::class, ImmutableShortDoubleMap::class),
    Triple(Short::class, Object::class, ImmutableShortObjectMap::class),
    Triple(Char::class, Boolean::class, ImmutableCharBooleanMap::class),
    Triple(Char::class, Byte::class, ImmutableCharByteMap::class),
    Triple(Char::class, Short::class, ImmutableCharShortMap::class),
    Triple(Char::class, Char::class, ImmutableCharCharMap::class),
    Triple(Char::class, Int::class, ImmutableCharIntMap::class),
    Triple(Char::class, Long::class, ImmutableCharLongMap::class),
    Triple(Char::class, Float::class, ImmutableCharFloatMap::class),
    Triple(Char::class, Double::class, ImmutableCharDoubleMap::class),
    Triple(Char::class, Object::class, ImmutableCharObjectMap::class),
    Triple(Int::class, Boolean::class, ImmutableIntBooleanMap::class),
    Triple(Int::class, Byte::class, ImmutableIntByteMap::class),
    Triple(Int::class, Short::class, ImmutableIntShortMap::class),
    Triple(Int::class, Char::class, ImmutableIntCharMap::class),
    Triple(Int::class, Int::class, ImmutableIntIntMap::class),
    Triple(Int::class, Long::class, ImmutableIntLongMap::class),
    Triple(Int::class, Float::class, ImmutableIntFloatMap::class),
    Triple(Int::class, Double::class, ImmutableIntDoubleMap::class),
    Triple(Int::class, Object::class, ImmutableIntObjectMap::class),
    Triple(Long::class, Boolean::class, ImmutableLongBooleanMap::class),
    Triple(Long::class, Byte::class, ImmutableLongByteMap::class),
    Triple(Long::class, Short::class, ImmutableLongShortMap::class),
    Triple(Long::class, Char::class, ImmutableLongCharMap::class),
    Triple(Long::class, Int::class, ImmutableLongIntMap::class),
    Triple(Long::class, Long::class, ImmutableLongLongMap::class),
    Triple(Long::class, Float::class, ImmutableLongFloatMap::class),
    Triple(Long::class, Double::class, ImmutableLongDoubleMap::class),
    Triple(Long::class, Object::class, ImmutableLongObjectMap::class),
//    Triple(Float::class, Boolean::class, ImmutableFloatBooleanMap::class),
//    Triple(Float::class, Byte::class, ImmutableFloatByteMap::class),
//    Triple(Float::class, Short::class, ImmutableFloatShortMap::class),
//    Triple(Float::class, Char::class, ImmutableFloatCharMap::class),
//    Triple(Float::class, Int::class, ImmutableFloatIntMap::class),
//    Triple(Float::class, Long::class, ImmutableFloatLongMap::class),
//    Triple(Float::class, Float::class, ImmutableFloatFloatMap::class),
//    Triple(Float::class, Double::class, ImmutableFloatDoubleMap::class),
//    Triple(Float::class, Object::class, ImmutableFloatObjectMap::class),
//    Triple(Double::class, Boolean::class, ImmutableDoubleBooleanMap::class),
//    Triple(Double::class, Byte::class, ImmutableDoubleByteMap::class),
//    Triple(Double::class, Short::class, ImmutableDoubleShortMap::class),
//    Triple(Double::class, Char::class, ImmutableDoubleCharMap::class),
//    Triple(Double::class, Int::class, ImmutableDoubleIntMap::class),
//    Triple(Double::class, Long::class, ImmutableDoubleLongMap::class),
//    Triple(Double::class, Float::class, ImmutableDoubleFloatMap::class),
//    Triple(Double::class, Double::class, ImmutableDoubleDoubleMap::class),
//    Triple(Double::class, Object::class, ImmutableDoubleObjectMap::class),
    Triple(Object::class, Boolean::class, ImmutableObjectBooleanMap::class),
    Triple(Object::class, Byte::class, ImmutableObjectByteMap::class),
    Triple(Object::class, Short::class, ImmutableObjectShortMap::class),
    Triple(Object::class, Char::class, ImmutableObjectCharMap::class),
    Triple(Object::class, Int::class, ImmutableObjectIntMap::class),
    Triple(Object::class, Long::class, ImmutableObjectLongMap::class),
    Triple(Object::class, Float::class, ImmutableObjectFloatMap::class),
    Triple(Object::class, Double::class, ImmutableObjectDoubleMap::class)
  )

  val mutableMapTypes = listOf(
    Triple(Byte::class, Boolean::class, MutableByteBooleanMap::class),
    Triple(Byte::class, Byte::class, MutableByteByteMap::class),
    Triple(Byte::class, Short::class, MutableByteShortMap::class),
    Triple(Byte::class, Char::class, MutableByteCharMap::class),
    Triple(Byte::class, Int::class, MutableByteIntMap::class),
    Triple(Byte::class, Long::class, MutableByteLongMap::class),
    Triple(Byte::class, Float::class, MutableByteFloatMap::class),
    Triple(Byte::class, Double::class, MutableByteDoubleMap::class),
    Triple(Byte::class, Object::class, MutableByteObjectMap::class),
    Triple(Short::class, Boolean::class, MutableShortBooleanMap::class),
    Triple(Short::class, Byte::class, MutableShortByteMap::class),
    Triple(Short::class, Short::class, MutableShortShortMap::class),
    Triple(Short::class, Char::class, MutableShortCharMap::class),
    Triple(Short::class, Int::class, MutableShortIntMap::class),
    Triple(Short::class, Long::class, MutableShortLongMap::class),
    Triple(Short::class, Float::class, MutableShortFloatMap::class),
    Triple(Short::class, Double::class, MutableShortDoubleMap::class),
    Triple(Short::class, Object::class, MutableShortObjectMap::class),
    Triple(Char::class, Boolean::class, MutableCharBooleanMap::class),
    Triple(Char::class, Byte::class, MutableCharByteMap::class),
    Triple(Char::class, Short::class, MutableCharShortMap::class),
    Triple(Char::class, Char::class, MutableCharCharMap::class),
    Triple(Char::class, Int::class, MutableCharIntMap::class),
    Triple(Char::class, Long::class, MutableCharLongMap::class),
    Triple(Char::class, Float::class, MutableCharFloatMap::class),
    Triple(Char::class, Double::class, MutableCharDoubleMap::class),
    Triple(Char::class, Object::class, MutableCharObjectMap::class),
    Triple(Int::class, Boolean::class, MutableIntBooleanMap::class),
    Triple(Int::class, Byte::class, MutableIntByteMap::class),
    Triple(Int::class, Short::class, MutableIntShortMap::class),
    Triple(Int::class, Char::class, MutableIntCharMap::class),
    Triple(Int::class, Int::class, MutableIntIntMap::class),
    Triple(Int::class, Long::class, MutableIntLongMap::class),
    Triple(Int::class, Float::class, MutableIntFloatMap::class),
    Triple(Int::class, Double::class, MutableIntDoubleMap::class),
    Triple(Int::class, Object::class, MutableIntObjectMap::class),
    Triple(Long::class, Boolean::class, MutableLongBooleanMap::class),
    Triple(Long::class, Byte::class, MutableLongByteMap::class),
    Triple(Long::class, Short::class, MutableLongShortMap::class),
    Triple(Long::class, Char::class, MutableLongCharMap::class),
    Triple(Long::class, Int::class, MutableLongIntMap::class),
    Triple(Long::class, Long::class, MutableLongLongMap::class),
    Triple(Long::class, Float::class, MutableLongFloatMap::class),
    Triple(Long::class, Double::class, MutableLongDoubleMap::class),
    Triple(Long::class, Object::class, MutableLongObjectMap::class),
//    Triple(Float::class, Boolean::class, MutableFloatBooleanMap::class),
//    Triple(Float::class, Byte::class, MutableFloatByteMap::class),
//    Triple(Float::class, Short::class, MutableFloatShortMap::class),
//    Triple(Float::class, Char::class, MutableFloatCharMap::class),
//    Triple(Float::class, Int::class, MutableFloatIntMap::class),
//    Triple(Float::class, Long::class, MutableFloatLongMap::class),
//    Triple(Float::class, Float::class, MutableFloatFloatMap::class),
//    Triple(Float::class, Double::class, MutableFloatDoubleMap::class),
//    Triple(Float::class, Object::class, MutableFloatObjectMap::class),
//    Triple(Double::class, Boolean::class, MutableDoubleBooleanMap::class),
//    Triple(Double::class, Byte::class, MutableDoubleByteMap::class),
//    Triple(Double::class, Short::class, MutableDoubleShortMap::class),
//    Triple(Double::class, Char::class, MutableDoubleCharMap::class),
//    Triple(Double::class, Int::class, MutableDoubleIntMap::class),
//    Triple(Double::class, Long::class, MutableDoubleLongMap::class),
//    Triple(Double::class, Float::class, MutableDoubleFloatMap::class),
//    Triple(Double::class, Double::class, MutableDoubleDoubleMap::class),
//    Triple(Double::class, Object::class, MutableDoubleObjectMap::class),
    Triple(Object::class, Boolean::class, MutableObjectBooleanMap::class),
    Triple(Object::class, Byte::class, MutableObjectByteMap::class),
    Triple(Object::class, Short::class, MutableObjectShortMap::class),
    Triple(Object::class, Char::class, MutableObjectCharMap::class),
    Triple(Object::class, Int::class, MutableObjectIntMap::class),
    Triple(Object::class, Long::class, MutableObjectLongMap::class),
    Triple(Object::class, Float::class, MutableObjectFloatMap::class),
    Triple(Object::class, Double::class, MutableObjectDoubleMap::class)
  )

  val unmodifiableSets = mapOf(
    Boolean::class to UnmodifiableBooleanSet::class,
    Byte::class to UnmodifiableByteSet::class,
    Short::class to UnmodifiableShortSet::class,
    Char::class to UnmodifiableCharSet::class,
    Int::class to UnmodifiableIntSet::class,
    Long::class to UnmodifiableLongSet::class,
    Float::class to UnmodifiableFloatSet::class,
    Double::class to UnmodifiableDoubleSet::class,
    Object::class to UnmodifiableMutableSet::class
  )

  val unmodifiableCollection = mapOf(
    Boolean::class to UnmodifiableBooleanCollection::class,
    Byte::class to UnmodifiableByteCollection::class,
    Short::class to UnmodifiableShortCollection::class,
    Char::class to UnmodifiableCharCollection::class,
    Int::class to UnmodifiableIntCollection::class,
    Long::class to UnmodifiableLongCollection::class,
    Float::class to UnmodifiableFloatCollection::class,
    Double::class to UnmodifiableDoubleCollection::class,
    Object::class to UnmodifiableMutableCollection::class
  )

  val mutableIterators = mapOf(
    Boolean::class to MutableBooleanIterator::class,
    Byte::class to MutableByteIterator::class,
    Short::class to MutableShortIterator::class,
    Char::class to MutableCharIterator::class,
    Int::class to MutableIntIterator::class,
    Long::class to MutableLongIterator::class,
    Float::class to MutableFloatIterator::class,
    Double::class to MutableDoubleIterator::class,
  )

  val pairs = mapOf(
    (Byte::class to Boolean::class) to ByteBooleanPair::class,
    (Byte::class to Byte::class) to ByteBytePair::class,
    (Byte::class to Short::class) to ByteShortPair::class,
    (Byte::class to Char::class) to ByteCharPair::class,
    (Byte::class to Int::class) to ByteIntPair::class,
    (Byte::class to Long::class) to ByteLongPair::class,
    (Byte::class to Float::class) to ByteFloatPair::class,
    (Byte::class to Double::class) to ByteDoublePair::class,
    (Byte::class to Object::class) to ByteObjectPair::class,
    (Short::class to Boolean::class) to ShortBooleanPair::class,
    (Short::class to Byte::class) to ShortBytePair::class,
    (Short::class to Short::class) to ShortShortPair::class,
    (Short::class to Char::class) to ShortCharPair::class,
    (Short::class to Int::class) to ShortIntPair::class,
    (Short::class to Long::class) to ShortLongPair::class,
    (Short::class to Float::class) to ShortFloatPair::class,
    (Short::class to Double::class) to ShortDoublePair::class,
    (Short::class to Object::class) to ShortObjectPair::class,
    (Char::class to Boolean::class) to CharBooleanPair::class,
    (Char::class to Byte::class) to CharBytePair::class,
    (Char::class to Short::class) to CharShortPair::class,
    (Char::class to Char::class) to CharCharPair::class,
    (Char::class to Int::class) to CharIntPair::class,
    (Char::class to Long::class) to CharLongPair::class,
    (Char::class to Float::class) to CharFloatPair::class,
    (Char::class to Double::class) to CharDoublePair::class,
    (Char::class to Object::class) to CharObjectPair::class,
    (Int::class to Boolean::class) to IntBooleanPair::class,
    (Int::class to Byte::class) to IntBytePair::class,
    (Int::class to Short::class) to IntShortPair::class,
    (Int::class to Char::class) to IntCharPair::class,
    (Int::class to Int::class) to IntIntPair::class,
    (Int::class to Long::class) to IntLongPair::class,
    (Int::class to Float::class) to IntFloatPair::class,
    (Int::class to Double::class) to IntDoublePair::class,
    (Int::class to Object::class) to IntObjectPair::class,
    (Long::class to Boolean::class) to LongBooleanPair::class,
    (Long::class to Byte::class) to LongBytePair::class,
    (Long::class to Short::class) to LongShortPair::class,
    (Long::class to Char::class) to LongCharPair::class,
    (Long::class to Int::class) to LongIntPair::class,
    (Long::class to Long::class) to LongLongPair::class,
    (Long::class to Float::class) to LongFloatPair::class,
    (Long::class to Double::class) to LongDoublePair::class,
    (Long::class to Object::class) to LongObjectPair::class,
    (Float::class to Boolean::class) to FloatBooleanPair::class,
    (Float::class to Byte::class) to FloatBytePair::class,
    (Float::class to Short::class) to FloatShortPair::class,
    (Float::class to Char::class) to FloatCharPair::class,
    (Float::class to Int::class) to FloatIntPair::class,
    (Float::class to Long::class) to FloatLongPair::class,
    (Float::class to Float::class) to FloatFloatPair::class,
    (Float::class to Double::class) to FloatDoublePair::class,
    (Float::class to Object::class) to FloatObjectPair::class,
    (Double::class to Boolean::class) to DoubleBooleanPair::class,
    (Double::class to Byte::class) to DoubleBytePair::class,
    (Double::class to Short::class) to DoubleShortPair::class,
    (Double::class to Char::class) to DoubleCharPair::class,
    (Double::class to Int::class) to DoubleIntPair::class,
    (Double::class to Long::class) to DoubleLongPair::class,
    (Double::class to Float::class) to DoubleFloatPair::class,
    (Double::class to Double::class) to DoubleDoublePair::class,
    (Double::class to Object::class) to DoubleObjectPair::class,
    (Object::class to Boolean::class) to ObjectBooleanPair::class,
    (Object::class to Byte::class) to ObjectBytePair::class,
    (Object::class to Short::class) to ObjectShortPair::class,
    (Object::class to Char::class) to ObjectCharPair::class,
    (Object::class to Int::class) to ObjectIntPair::class,
    (Object::class to Long::class) to ObjectLongPair::class,
    (Object::class to Float::class) to ObjectFloatPair::class,
    (Object::class to Double::class) to ObjectDoublePair::class
  )

  val immutableListFactoryTypes = listOf(
    Boolean::class to ImmutableBooleanListFactory::class,
    Byte::class to ImmutableByteListFactory::class,
    Short::class to ImmutableShortListFactory::class,
    Char::class to ImmutableCharListFactory::class,
    Int::class to ImmutableIntListFactory::class,
    Long::class to ImmutableLongListFactory::class,
    Float::class to ImmutableFloatListFactory::class,
    Double::class to ImmutableDoubleListFactory::class,
  )

  val immutableMapFactoryTypes = listOf(
    Triple(Byte::class, Boolean::class, ImmutableByteBooleanMapFactory::class),
    Triple(Byte::class, Byte::class, ImmutableByteByteMapFactory::class),
    Triple(Byte::class, Short::class, ImmutableByteShortMapFactory::class),
    Triple(Byte::class, Char::class, ImmutableByteCharMapFactory::class),
    Triple(Byte::class, Int::class, ImmutableByteIntMapFactory::class),
    Triple(Byte::class, Long::class, ImmutableByteLongMapFactory::class),
    Triple(Byte::class, Float::class, ImmutableByteFloatMapFactory::class),
    Triple(Byte::class, Double::class, ImmutableByteDoubleMapFactory::class),
    Triple(Byte::class, Object::class, ImmutableByteObjectMapFactory::class),
    Triple(Short::class, Boolean::class, ImmutableShortBooleanMapFactory::class),
    Triple(Short::class, Byte::class, ImmutableShortByteMapFactory::class),
    Triple(Short::class, Short::class, ImmutableShortShortMapFactory::class),
    Triple(Short::class, Char::class, ImmutableShortCharMapFactory::class),
    Triple(Short::class, Int::class, ImmutableShortIntMapFactory::class),
    Triple(Short::class, Long::class, ImmutableShortLongMapFactory::class),
    Triple(Short::class, Float::class, ImmutableShortFloatMapFactory::class),
    Triple(Short::class, Double::class, ImmutableShortDoubleMapFactory::class),
    Triple(Short::class, Object::class, ImmutableShortObjectMapFactory::class),
    Triple(Char::class, Boolean::class, ImmutableCharBooleanMapFactory::class),
    Triple(Char::class, Byte::class, ImmutableCharByteMapFactory::class),
    Triple(Char::class, Short::class, ImmutableCharShortMapFactory::class),
    Triple(Char::class, Char::class, ImmutableCharCharMapFactory::class),
    Triple(Char::class, Int::class, ImmutableCharIntMapFactory::class),
    Triple(Char::class, Long::class, ImmutableCharLongMapFactory::class),
    Triple(Char::class, Float::class, ImmutableCharFloatMapFactory::class),
    Triple(Char::class, Double::class, ImmutableCharDoubleMapFactory::class),
    Triple(Char::class, Object::class, ImmutableCharObjectMapFactory::class),
    Triple(Int::class, Boolean::class, ImmutableIntBooleanMapFactory::class),
    Triple(Int::class, Byte::class, ImmutableIntByteMapFactory::class),
    Triple(Int::class, Short::class, ImmutableIntShortMapFactory::class),
    Triple(Int::class, Char::class, ImmutableIntCharMapFactory::class),
    Triple(Int::class, Int::class, ImmutableIntIntMapFactory::class),
    Triple(Int::class, Long::class, ImmutableIntLongMapFactory::class),
    Triple(Int::class, Float::class, ImmutableIntFloatMapFactory::class),
    Triple(Int::class, Double::class, ImmutableIntDoubleMapFactory::class),
    Triple(Int::class, Object::class, ImmutableIntObjectMapFactory::class),
    Triple(Long::class, Boolean::class, ImmutableLongBooleanMapFactory::class),
    Triple(Long::class, Byte::class, ImmutableLongByteMapFactory::class),
    Triple(Long::class, Short::class, ImmutableLongShortMapFactory::class),
    Triple(Long::class, Char::class, ImmutableLongCharMapFactory::class),
    Triple(Long::class, Int::class, ImmutableLongIntMapFactory::class),
    Triple(Long::class, Long::class, ImmutableLongLongMapFactory::class),
    Triple(Long::class, Float::class, ImmutableLongFloatMapFactory::class),
    Triple(Long::class, Double::class, ImmutableLongDoubleMapFactory::class),
    Triple(Long::class, Object::class, ImmutableLongObjectMapFactory::class),
    Triple(Object::class, Boolean::class, ImmutableObjectBooleanMapFactory::class),
    Triple(Object::class, Byte::class, ImmutableObjectByteMapFactory::class),
    Triple(Object::class, Short::class, ImmutableObjectShortMapFactory::class),
    Triple(Object::class, Char::class, ImmutableObjectCharMapFactory::class),
    Triple(Object::class, Int::class, ImmutableObjectIntMapFactory::class),
    Triple(Object::class, Long::class, ImmutableObjectLongMapFactory::class),
    Triple(Object::class, Float::class, ImmutableObjectFloatMapFactory::class),
    Triple(Object::class, Double::class, ImmutableObjectDoubleMapFactory::class)
  )

  private fun keyValueTypes(): ArrayList<Pair<KClass<*>, KClass<*>>> {
    val types = primitiveTypes + Object::class
    val result = arrayListOf<Pair<KClass<*>, KClass<*>>>()
    for (keyType in types) {
      if (keyType == Boolean::class) {
        continue
      }
      for (valueType in types) {
        if (keyType == Object::class && keyType == valueType) {
          continue
        }
        result += keyType to valueType
      }
    }
    return result
  }

  fun nullableTypeOf(kclass: KClass<*>): TypeName {
    return if (kclass == Object::class) Object::class.asTypeName().copy(true) else kclass.asTypeName()
  }

  fun simpleNameOf(kclass: KClass<*>): String {
    return if (kclass == Object::class) "Object" else kclass.simpleName!!
  }

  fun mutableIteratorOf(kclass: KClass<*>): TypeName {
    return ClassName.bestGuess("kotlin.collections.MutableIterator").parameterizedBy(kclass.asClassName())
  }

  fun mutableIteratorOf(typeName: TypeName): TypeName {
    return ClassName.bestGuess("kotlin.collections.MutableIterator").parameterizedBy(typeName)
  }

  fun mutableSetOf(elementType: KClass<*>): TypeName {
    return ClassName.bestGuess("kotlin.collections.MutableSet").parameterizedBy(elementType.asTypeName())
  }

  fun mutableSetOf(elementType: TypeName): TypeName {
    return ClassName.bestGuess("kotlin.collections.MutableSet").parameterizedBy(elementType)
  }

  fun mutableCollectionOf(elementType: KClass<*>): TypeName {
    return ClassName.bestGuess("kotlin.collections.MutableCollection").parameterizedBy(elementType.asTypeName())
  }

  fun mutableCollectionOf(elementType: TypeName): TypeName {
    return ClassName.bestGuess("kotlin.collections.MutableCollection").parameterizedBy(elementType)
  }

  fun mutableEntryOf(keyType: KClass<*>, valueType: KClass<*>): TypeName {
    return ClassName.bestGuess("kotlin.collections.MutableMap.MutableEntry")
      .parameterizedBy(keyType.asTypeName(), valueType.asTypeName())
  }

  fun mutableEntryOf(keyType: TypeName, valueType: TypeName): TypeName {
    return ClassName.bestGuess("kotlin.collections.MutableMap.MutableEntry")
      .parameterizedBy(keyType, valueType)
  }

  fun isObj(kclass: KClass<*>) = kclass == Object::class

  fun pairOf(keyType: KClass<*>, valueType: KClass<*>): KClass<*> {
    return pairs[keyType to valueType]!!
  }

  @JvmStatic
  fun main(args: Array<String>) {
//    println(mutableIteratorOf(Byte::class))
    keyValueTypes().forEach { (k, v) ->
      val keyTypeName = simpleNameOf(k)
      val valueTypeName = simpleNameOf(v)
//      println("Triple(${keyTypeName}::class, ${valueTypeName}::class, Immutable${keyTypeName}${valueTypeName}Map::class),")
      println("(${keyTypeName}::class to ${valueTypeName}::class) to ${keyTypeName}${valueTypeName}Pair::class,")
    }
  }
}
