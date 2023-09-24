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

  val primitiveArrayTypes = mapOf(
    Boolean::class to BOOLEAN_ARRAY,
    Byte::class to BYTE_ARRAY,
    Short::class to SHORT_ARRAY,
    Char::class to CHAR_ARRAY,
    Int::class to INT_ARRAY,
    Long::class to LONG_ARRAY,
    Float::class to FLOAT_ARRAY,
    Double::class to DOUBLE_ARRAY
  )

  val iteratorTypes = mapOf(
    Boolean::class to BooleanIterator::class,
    Byte::class to ByteIterator::class,
    Short::class to ShortIterator::class,
    Char::class to CharIterator::class,
    Int::class to IntIterator::class,
    Long::class to LongIterator::class,
    Float::class to FloatIterator::class,
    Double::class to DoubleIterator::class
  )

  val immutableCollectionTypes = mapOf(
    Boolean::class to ImmutableBooleanCollection::class,
    Byte::class to ImmutableByteCollection::class,
    Short::class to ImmutableShortCollection::class,
    Char::class to ImmutableCharCollection::class,
    Int::class to ImmutableIntCollection::class,
    Long::class to ImmutableLongCollection::class,
    Float::class to ImmutableFloatCollection::class,
    Double::class to ImmutableDoubleCollection::class,
  )

  val mutableCollectionTypes = mapOf(
    Boolean::class to MutableBooleanCollection::class,
    Byte::class to MutableByteCollection::class,
    Short::class to MutableShortCollection::class,
    Char::class to MutableCharCollection::class,
    Int::class to MutableIntCollection::class,
    Long::class to MutableLongCollection::class,
    Float::class to MutableFloatCollection::class,
    Double::class to MutableDoubleCollection::class,
  )

  val listTypes = mapOf(
    Boolean::class to BooleanList::class,
    Byte::class to ByteList::class,
    Short::class to ShortList::class,
    Char::class to CharList::class,
    Int::class to IntList::class,
    Long::class to LongList::class,
    Float::class to FloatList::class,
    Double::class to DoubleList::class,
  )

  val immutableListTypes = mapOf(
    Boolean::class to ImmutableBooleanList::class,
    Byte::class to ImmutableByteList::class,
    Short::class to ImmutableShortList::class,
    Char::class to ImmutableCharList::class,
    Int::class to ImmutableIntList::class,
    Long::class to ImmutableLongList::class,
    Float::class to ImmutableFloatList::class,
    Double::class to ImmutableDoubleList::class,
  )

  val mutableListTypes = mapOf(
    Boolean::class to MutableBooleanList::class,
    Byte::class to MutableByteList::class,
    Short::class to MutableShortList::class,
    Char::class to MutableCharList::class,
    Int::class to MutableIntList::class,
    Long::class to MutableLongList::class,
    Float::class to MutableFloatList::class,
    Double::class to MutableDoubleList::class,
  )

  val setTypes = mapOf(
    Boolean::class to BooleanSet::class,
    Byte::class to ByteSet::class,
    Short::class to ShortSet::class,
    Char::class to CharSet::class,
    Int::class to IntSet::class,
    Long::class to LongSet::class
  )

  val immutableSetTypes = mapOf(
    Boolean::class to ImmutableBooleanSet::class,
    Byte::class to ImmutableByteSet::class,
    Short::class to ImmutableShortSet::class,
    Char::class to ImmutableCharSet::class,
    Int::class to ImmutableIntSet::class,
    Long::class to ImmutableLongSet::class
  )

  val mutableSetTypes = mapOf(
    Boolean::class to MutableBooleanSet::class,
    Byte::class to MutableByteSet::class,
    Short::class to MutableShortSet::class,
    Char::class to MutableCharSet::class,
    Int::class to MutableIntSet::class,
    Long::class to MutableLongSet::class
  )

  val mapTypes = mapOf(
    (Byte::class to Boolean::class) to ByteBooleanMap::class,
    (Byte::class to Byte::class) to ByteByteMap::class,
    (Byte::class to Short::class) to ByteShortMap::class,
    (Byte::class to Char::class) to ByteCharMap::class,
    (Byte::class to Int::class) to ByteIntMap::class,
    (Byte::class to Long::class) to ByteLongMap::class,
    (Byte::class to Float::class) to ByteFloatMap::class,
    (Byte::class to Double::class) to ByteDoubleMap::class,
    (Byte::class to Object::class) to ByteObjectMap::class,
    (Short::class to Boolean::class) to ShortBooleanMap::class,
    (Short::class to Byte::class) to ShortByteMap::class,
    (Short::class to Short::class) to ShortShortMap::class,
    (Short::class to Char::class) to ShortCharMap::class,
    (Short::class to Int::class) to ShortIntMap::class,
    (Short::class to Long::class) to ShortLongMap::class,
    (Short::class to Float::class) to ShortFloatMap::class,
    (Short::class to Double::class) to ShortDoubleMap::class,
    (Short::class to Object::class) to ShortObjectMap::class,
    (Char::class to Boolean::class) to CharBooleanMap::class,
    (Char::class to Byte::class) to CharByteMap::class,
    (Char::class to Short::class) to CharShortMap::class,
    (Char::class to Char::class) to CharCharMap::class,
    (Char::class to Int::class) to CharIntMap::class,
    (Char::class to Long::class) to CharLongMap::class,
    (Char::class to Float::class) to CharFloatMap::class,
    (Char::class to Double::class) to CharDoubleMap::class,
    (Char::class to Object::class) to CharObjectMap::class,
    (Int::class to Boolean::class) to IntBooleanMap::class,
    (Int::class to Byte::class) to IntByteMap::class,
    (Int::class to Short::class) to IntShortMap::class,
    (Int::class to Char::class) to IntCharMap::class,
    (Int::class to Int::class) to IntIntMap::class,
    (Int::class to Long::class) to IntLongMap::class,
    (Int::class to Float::class) to IntFloatMap::class,
    (Int::class to Double::class) to IntDoubleMap::class,
    (Int::class to Object::class) to IntObjectMap::class,
    (Long::class to Boolean::class) to LongBooleanMap::class,
    (Long::class to Byte::class) to LongByteMap::class,
    (Long::class to Short::class) to LongShortMap::class,
    (Long::class to Char::class) to LongCharMap::class,
    (Long::class to Int::class) to LongIntMap::class,
    (Long::class to Long::class) to LongLongMap::class,
    (Long::class to Float::class) to LongFloatMap::class,
    (Long::class to Double::class) to LongDoubleMap::class,
    (Long::class to Object::class) to LongObjectMap::class,
//    (Float::class to Boolean::class) to FloatBooleanMap::class,
//    (Float::class to Byte::class) to FloatByteMap::class,
//    (Float::class to Short::class) to FloatShortMap::class,
//    (Float::class to Char::class) to FloatCharMap::class,
//    (Float::class to Int::class) to FloatIntMap::class,
//    (Float::class to Long::class) to FloatLongMap::class,
//    (Float::class to Float::class) to FloatFloatMap::class,
//    (Float::class to Double::class) to FloatDoubleMap::class,
//    (Float::class to Object::class) to FloatObjectMap::class,
//    (Double::class to Boolean::class) to DoubleBooleanMap::class,
//    (Double::class to Byte::class) to DoubleByteMap::class,
//    (Double::class to Short::class) to DoubleShortMap::class,
//    (Double::class to Char::class) to DoubleCharMap::class,
//    (Double::class to Int::class) to DoubleIntMap::class,
//    (Double::class to Long::class) to DoubleLongMap::class,
//    (Double::class to Float::class) to DoubleFloatMap::class,
//    (Double::class to Double::class) to DoubleDoubleMap::class,
//    (Double::class to Object::class) to DoubleObjectMap::class,
    (Object::class to Boolean::class) to ObjectBooleanMap::class,
    (Object::class to Byte::class) to ObjectByteMap::class,
    (Object::class to Short::class) to ObjectShortMap::class,
    (Object::class to Char::class) to ObjectCharMap::class,
    (Object::class to Int::class) to ObjectIntMap::class,
    (Object::class to Long::class) to ObjectLongMap::class,
    (Object::class to Float::class) to ObjectFloatMap::class,
    (Object::class to Double::class) to ObjectDoubleMap::class
  )

  val immutableMapTypes = mapOf(
    (Byte::class to Boolean::class) to ImmutableByteBooleanMap::class,
    (Byte::class to Byte::class) to ImmutableByteByteMap::class,
    (Byte::class to Short::class) to ImmutableByteShortMap::class,
    (Byte::class to Char::class) to ImmutableByteCharMap::class,
    (Byte::class to Int::class) to ImmutableByteIntMap::class,
    (Byte::class to Long::class) to ImmutableByteLongMap::class,
    (Byte::class to Float::class) to ImmutableByteFloatMap::class,
    (Byte::class to Double::class) to ImmutableByteDoubleMap::class,
    (Byte::class to Object::class) to ImmutableByteObjectMap::class,
    (Short::class to Boolean::class) to ImmutableShortBooleanMap::class,
    (Short::class to Byte::class) to ImmutableShortByteMap::class,
    (Short::class to Short::class) to ImmutableShortShortMap::class,
    (Short::class to Char::class) to ImmutableShortCharMap::class,
    (Short::class to Int::class) to ImmutableShortIntMap::class,
    (Short::class to Long::class) to ImmutableShortLongMap::class,
    (Short::class to Float::class) to ImmutableShortFloatMap::class,
    (Short::class to Double::class) to ImmutableShortDoubleMap::class,
    (Short::class to Object::class) to ImmutableShortObjectMap::class,
    (Char::class to Boolean::class) to ImmutableCharBooleanMap::class,
    (Char::class to Byte::class) to ImmutableCharByteMap::class,
    (Char::class to Short::class) to ImmutableCharShortMap::class,
    (Char::class to Char::class) to ImmutableCharCharMap::class,
    (Char::class to Int::class) to ImmutableCharIntMap::class,
    (Char::class to Long::class) to ImmutableCharLongMap::class,
    (Char::class to Float::class) to ImmutableCharFloatMap::class,
    (Char::class to Double::class) to ImmutableCharDoubleMap::class,
    (Char::class to Object::class) to ImmutableCharObjectMap::class,
    (Int::class to Boolean::class) to ImmutableIntBooleanMap::class,
    (Int::class to Byte::class) to ImmutableIntByteMap::class,
    (Int::class to Short::class) to ImmutableIntShortMap::class,
    (Int::class to Char::class) to ImmutableIntCharMap::class,
    (Int::class to Int::class) to ImmutableIntIntMap::class,
    (Int::class to Long::class) to ImmutableIntLongMap::class,
    (Int::class to Float::class) to ImmutableIntFloatMap::class,
    (Int::class to Double::class) to ImmutableIntDoubleMap::class,
    (Int::class to Object::class) to ImmutableIntObjectMap::class,
    (Long::class to Boolean::class) to ImmutableLongBooleanMap::class,
    (Long::class to Byte::class) to ImmutableLongByteMap::class,
    (Long::class to Short::class) to ImmutableLongShortMap::class,
    (Long::class to Char::class) to ImmutableLongCharMap::class,
    (Long::class to Int::class) to ImmutableLongIntMap::class,
    (Long::class to Long::class) to ImmutableLongLongMap::class,
    (Long::class to Float::class) to ImmutableLongFloatMap::class,
    (Long::class to Double::class) to ImmutableLongDoubleMap::class,
    (Long::class to Object::class) to ImmutableLongObjectMap::class,
//    (Float::class to Boolean::class) to ImmutableFloatBooleanMap::class,
//    (Float::class to Byte::class) to ImmutableFloatByteMap::class,
//    (Float::class to Short::class) to ImmutableFloatShortMap::class,
//    (Float::class to Char::class) to ImmutableFloatCharMap::class,
//    (Float::class to Int::class) to ImmutableFloatIntMap::class,
//    (Float::class to Long::class) to ImmutableFloatLongMap::class,
//    (Float::class to Float::class) to ImmutableFloatFloatMap::class,
//    (Float::class to Double::class) to ImmutableFloatDoubleMap::class,
//    (Float::class to Object::class) to ImmutableFloatObjectMap::class,
//    (Double::class to Boolean::class) to ImmutableDoubleBooleanMap::class,
//    (Double::class to Byte::class) to ImmutableDoubleByteMap::class,
//    (Double::class to Short::class) to ImmutableDoubleShortMap::class,
//    (Double::class to Char::class) to ImmutableDoubleCharMap::class,
//    (Double::class to Int::class) to ImmutableDoubleIntMap::class,
//    (Double::class to Long::class) to ImmutableDoubleLongMap::class,
//    (Double::class to Float::class) to ImmutableDoubleFloatMap::class,
//    (Double::class to Double::class) to ImmutableDoubleDoubleMap::class,
//    (Double::class to Object::class) to ImmutableDoubleObjectMap::class,
    (Object::class to Boolean::class) to ImmutableObjectBooleanMap::class,
    (Object::class to Byte::class) to ImmutableObjectByteMap::class,
    (Object::class to Short::class) to ImmutableObjectShortMap::class,
    (Object::class to Char::class) to ImmutableObjectCharMap::class,
    (Object::class to Int::class) to ImmutableObjectIntMap::class,
    (Object::class to Long::class) to ImmutableObjectLongMap::class,
    (Object::class to Float::class) to ImmutableObjectFloatMap::class,
    (Object::class to Double::class) to ImmutableObjectDoubleMap::class
  )

  val mutableMapTypes = mapOf(
    (Byte::class to Boolean::class) to MutableByteBooleanMap::class,
    (Byte::class to Byte::class) to MutableByteByteMap::class,
    (Byte::class to Short::class) to MutableByteShortMap::class,
    (Byte::class to Char::class) to MutableByteCharMap::class,
    (Byte::class to Int::class) to MutableByteIntMap::class,
    (Byte::class to Long::class) to MutableByteLongMap::class,
    (Byte::class to Float::class) to MutableByteFloatMap::class,
    (Byte::class to Double::class) to MutableByteDoubleMap::class,
    (Byte::class to Object::class) to MutableByteObjectMap::class,
    (Short::class to Boolean::class) to MutableShortBooleanMap::class,
    (Short::class to Byte::class) to MutableShortByteMap::class,
    (Short::class to Short::class) to MutableShortShortMap::class,
    (Short::class to Char::class) to MutableShortCharMap::class,
    (Short::class to Int::class) to MutableShortIntMap::class,
    (Short::class to Long::class) to MutableShortLongMap::class,
    (Short::class to Float::class) to MutableShortFloatMap::class,
    (Short::class to Double::class) to MutableShortDoubleMap::class,
    (Short::class to Object::class) to MutableShortObjectMap::class,
    (Char::class to Boolean::class) to MutableCharBooleanMap::class,
    (Char::class to Byte::class) to MutableCharByteMap::class,
    (Char::class to Short::class) to MutableCharShortMap::class,
    (Char::class to Char::class) to MutableCharCharMap::class,
    (Char::class to Int::class) to MutableCharIntMap::class,
    (Char::class to Long::class) to MutableCharLongMap::class,
    (Char::class to Float::class) to MutableCharFloatMap::class,
    (Char::class to Double::class) to MutableCharDoubleMap::class,
    (Char::class to Object::class) to MutableCharObjectMap::class,
    (Int::class to Boolean::class) to MutableIntBooleanMap::class,
    (Int::class to Byte::class) to MutableIntByteMap::class,
    (Int::class to Short::class) to MutableIntShortMap::class,
    (Int::class to Char::class) to MutableIntCharMap::class,
    (Int::class to Int::class) to MutableIntIntMap::class,
    (Int::class to Long::class) to MutableIntLongMap::class,
    (Int::class to Float::class) to MutableIntFloatMap::class,
    (Int::class to Double::class) to MutableIntDoubleMap::class,
    (Int::class to Object::class) to MutableIntObjectMap::class,
    (Long::class to Boolean::class) to MutableLongBooleanMap::class,
    (Long::class to Byte::class) to MutableLongByteMap::class,
    (Long::class to Short::class) to MutableLongShortMap::class,
    (Long::class to Char::class) to MutableLongCharMap::class,
    (Long::class to Int::class) to MutableLongIntMap::class,
    (Long::class to Long::class) to MutableLongLongMap::class,
    (Long::class to Float::class) to MutableLongFloatMap::class,
    (Long::class to Double::class) to MutableLongDoubleMap::class,
    (Long::class to Object::class) to MutableLongObjectMap::class,
//    (Float::class to Boolean::class) to MutableFloatBooleanMap::class,
//    (Float::class to Byte::class) to MutableFloatByteMap::class,
//    (Float::class to Short::class) to MutableFloatShortMap::class,
//    (Float::class to Char::class) to MutableFloatCharMap::class,
//    (Float::class to Int::class) to MutableFloatIntMap::class,
//    (Float::class to Long::class) to MutableFloatLongMap::class,
//    (Float::class to Float::class) to MutableFloatFloatMap::class,
//    (Float::class to Double::class) to MutableFloatDoubleMap::class,
//    (Float::class to Object::class) to MutableFloatObjectMap::class,
//    (Double::class to Boolean::class) to MutableDoubleBooleanMap::class,
//    (Double::class to Byte::class) to MutableDoubleByteMap::class,
//    (Double::class to Short::class) to MutableDoubleShortMap::class,
//    (Double::class to Char::class) to MutableDoubleCharMap::class,
//    (Double::class to Int::class) to MutableDoubleIntMap::class,
//    (Double::class to Long::class) to MutableDoubleLongMap::class,
//    (Double::class to Float::class) to MutableDoubleFloatMap::class,
//    (Double::class to Double::class) to MutableDoubleDoubleMap::class,
//    (Double::class to Object::class) to MutableDoubleObjectMap::class,
    (Object::class to Boolean::class) to MutableObjectBooleanMap::class,
    (Object::class to Byte::class) to MutableObjectByteMap::class,
    (Object::class to Short::class) to MutableObjectShortMap::class,
    (Object::class to Char::class) to MutableObjectCharMap::class,
    (Object::class to Int::class) to MutableObjectIntMap::class,
    (Object::class to Long::class) to MutableObjectLongMap::class,
    (Object::class to Float::class) to MutableObjectFloatMap::class,
    (Object::class to Double::class) to MutableObjectDoubleMap::class
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

  val pairs = mapOf<Pair<KClass<*>, KClass<*>>, KClass<*>>(
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

  val immutableListFactoryTypes = mapOf(
    Boolean::class to ImmutableBooleanListFactory::class,
    Byte::class to ImmutableByteListFactory::class,
    Short::class to ImmutableShortListFactory::class,
    Char::class to ImmutableCharListFactory::class,
    Int::class to ImmutableIntListFactory::class,
    Long::class to ImmutableLongListFactory::class,
    Float::class to ImmutableFloatListFactory::class,
    Double::class to ImmutableDoubleListFactory::class,
  )

  val immutableMapFactoryTypes = mapOf(
    (Byte::class to Boolean::class) to ImmutableByteBooleanMapFactory::class,
    (Byte::class to Byte::class) to ImmutableByteByteMapFactory::class,
    (Byte::class to Short::class) to ImmutableByteShortMapFactory::class,
    (Byte::class to Char::class) to ImmutableByteCharMapFactory::class,
    (Byte::class to Int::class) to ImmutableByteIntMapFactory::class,
    (Byte::class to Long::class) to ImmutableByteLongMapFactory::class,
    (Byte::class to Float::class) to ImmutableByteFloatMapFactory::class,
    (Byte::class to Double::class) to ImmutableByteDoubleMapFactory::class,
    (Byte::class to Object::class) to ImmutableByteObjectMapFactory::class,
    (Short::class to Boolean::class) to ImmutableShortBooleanMapFactory::class,
    (Short::class to Byte::class) to ImmutableShortByteMapFactory::class,
    (Short::class to Short::class) to ImmutableShortShortMapFactory::class,
    (Short::class to Char::class) to ImmutableShortCharMapFactory::class,
    (Short::class to Int::class) to ImmutableShortIntMapFactory::class,
    (Short::class to Long::class) to ImmutableShortLongMapFactory::class,
    (Short::class to Float::class) to ImmutableShortFloatMapFactory::class,
    (Short::class to Double::class) to ImmutableShortDoubleMapFactory::class,
    (Short::class to Object::class) to ImmutableShortObjectMapFactory::class,
    (Char::class to Boolean::class) to ImmutableCharBooleanMapFactory::class,
    (Char::class to Byte::class) to ImmutableCharByteMapFactory::class,
    (Char::class to Short::class) to ImmutableCharShortMapFactory::class,
    (Char::class to Char::class) to ImmutableCharCharMapFactory::class,
    (Char::class to Int::class) to ImmutableCharIntMapFactory::class,
    (Char::class to Long::class) to ImmutableCharLongMapFactory::class,
    (Char::class to Float::class) to ImmutableCharFloatMapFactory::class,
    (Char::class to Double::class) to ImmutableCharDoubleMapFactory::class,
    (Char::class to Object::class) to ImmutableCharObjectMapFactory::class,
    (Int::class to Boolean::class) to ImmutableIntBooleanMapFactory::class,
    (Int::class to Byte::class) to ImmutableIntByteMapFactory::class,
    (Int::class to Short::class) to ImmutableIntShortMapFactory::class,
    (Int::class to Char::class) to ImmutableIntCharMapFactory::class,
    (Int::class to Int::class) to ImmutableIntIntMapFactory::class,
    (Int::class to Long::class) to ImmutableIntLongMapFactory::class,
    (Int::class to Float::class) to ImmutableIntFloatMapFactory::class,
    (Int::class to Double::class) to ImmutableIntDoubleMapFactory::class,
    (Int::class to Object::class) to ImmutableIntObjectMapFactory::class,
    (Long::class to Boolean::class) to ImmutableLongBooleanMapFactory::class,
    (Long::class to Byte::class) to ImmutableLongByteMapFactory::class,
    (Long::class to Short::class) to ImmutableLongShortMapFactory::class,
    (Long::class to Char::class) to ImmutableLongCharMapFactory::class,
    (Long::class to Int::class) to ImmutableLongIntMapFactory::class,
    (Long::class to Long::class) to ImmutableLongLongMapFactory::class,
    (Long::class to Float::class) to ImmutableLongFloatMapFactory::class,
    (Long::class to Double::class) to ImmutableLongDoubleMapFactory::class,
    (Long::class to Object::class) to ImmutableLongObjectMapFactory::class,
    (Object::class to Boolean::class) to ImmutableObjectBooleanMapFactory::class,
    (Object::class to Byte::class) to ImmutableObjectByteMapFactory::class,
    (Object::class to Short::class) to ImmutableObjectShortMapFactory::class,
    (Object::class to Char::class) to ImmutableObjectCharMapFactory::class,
    (Object::class to Int::class) to ImmutableObjectIntMapFactory::class,
    (Object::class to Long::class) to ImmutableObjectLongMapFactory::class,
    (Object::class to Float::class) to ImmutableObjectFloatMapFactory::class,
    (Object::class to Double::class) to ImmutableObjectDoubleMapFactory::class
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
//      println("(${keyTypeName}::class to ${valueTypeName}::class) to Immutable${keyTypeName}${valueTypeName}Map::class,")
      println("(${keyTypeName}::class to ${valueTypeName}::class) to ${keyTypeName}${valueTypeName}Pair::class,")
    }
  }
}
