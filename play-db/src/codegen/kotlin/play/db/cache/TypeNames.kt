package play.db.cache

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.plusParameter

typealias TypeVar = TypeVariableName

val Entity = ClassName.bestGuess("play.db.Entity")
val EntityInt = ClassName.bestGuess("play.db.EntityInt")
val EntityLong = ClassName.bestGuess("play.db.EntityLong")

val EntityCache = ClassName.bestGuess("play.db.cache.EntityCache")

// ID : Any
val ID_Any = TypeVariableName("ID", Any::class)

// E : Entity<ID>
val E_Entity_ID = TypeVariableName("E", Entity.plusParameter(TypeVariableName("ID")))

val ID = TypeVar("ID")
val E = TypeVar("E")

// EntityInt<E>
val EntityInt_E = TypeVariableName("E", EntityInt)

// EntityLong<E>
val EntityLong_E = TypeVariableName("E", EntityLong)

// Class<E>
val Class_E = ClassName.bestGuess("java.lang.Class").parameterizedBy(TypeVar("E"))

val EntityCacheInt = ClassName.bestGuess("play.db.cache.EntityCacheInt")
val EntityCacheLong = ClassName.bestGuess("play.db.cache.EntityCacheLong")

val PersistService = ClassName.bestGuess("play.db.PersistService")
val QueryService = ClassName.bestGuess("play.db.QueryService")

val Injector = ClassName.bestGuess("play.inject.Injector")
val Scheduler = ClassName.bestGuess("play.util.scheduling.Scheduler")
val DbExecutor = ClassName.bestGuess("play.db.DbExecutor")

val EntityCacheConf = ClassName.bestGuess("play.db.cache.AbstractEntityCacheFactory.Config")

val EntityCacheHelper = ClassName.bestGuess("play.db.cache.EntityCacheHelper")

val ConcurrentIntObjectMap = ClassName.bestGuess("play.util.collection.ConcurrentIntObjectMap")
val ConcurrentLongObjectMap = ClassName.bestGuess("play.util.collection.ConcurrentLongObjectMap")

val CacheObj = ClassName.bestGuess("CacheObj")

val ConcurrentMap_ID_E =
  ClassName.bestGuess("java.util.concurrent.ConcurrentMap").plusParameter(ID).plusParameter(E)
val ConcurrentMap_ID_CacheObj_ID_E =
  ClassName.bestGuess("java.util.concurrent.ConcurrentMap").plusParameter(ID)
    .plusParameter(CacheObj.parameterizedBy(ID).plusParameter(E))
val ConcurrentHashMap = ClassName.bestGuess("java.util.concurrent.ConcurrentHashMap")

val ConcurrentIntObjectMap_E =
  ClassName.bestGuess("play.util.collection.ConcurrentIntObjectMap").plusParameter(E)
val ConcurrentLongObjectMap_E =
  ClassName.bestGuess("play.util.collection.ConcurrentLongObjectMap").plusParameter(E)

val EntityInt_CacheObj = ClassName.bestGuess("CacheObj").plusParameter(TypeVar("E", EntityInt))
val ConcurrentIntObjectMap_CacheObj_ID_E =
  ClassName.bestGuess("play.util.collection.ConcurrentIntObjectMap").plusParameter(EntityInt_CacheObj)
val EntityLong_CacheObj = ClassName.bestGuess("CacheObj").plusParameter(TypeVar("E", EntityLong))
val ConcurrentLongObjectMap_CacheObj_ID_E =
  ClassName.bestGuess("play.util.collection.ConcurrentLongObjectMap").plusParameter(EntityLong_CacheObj)

val NonBlockingHashSetInt = ClassName.bestGuess("play.util.collection.NonBlockingHashSetInt")
val NonBlockingHashSetLong = ClassName.bestGuess("play.util.collection.NonBlockingHashSetLong")
val MutableSet_ID = ClassName.bestGuess("kotlin.collections.MutableSet").parameterizedBy(ID)

val ConcurrentHashSet = ClassName.bestGuess("play.util.collection.ConcurrentHashSet")

val CacheSpec = ClassName.bestGuess("play.db.cache.CacheSpec")

val NeverExpireEvaluator = ClassName.bestGuess("play.db.cache.NeverExpireEvaluator")

val currentMillis = MemberName("play.util.time", "currentMillis")

val seconds = MemberName("kotlin.time", "seconds")

val minutes = MemberName("kotlin.time", "minutes")

val Log = ClassName.bestGuess("play.Log")

val Nullable = ClassName.bestGuess("javax.annotation.Nullable")

val IntToObjFunction_E = ClassName.bestGuess("play.util.function.IntToObjFunction").plusParameter(E)
val IntToObjFunction_E_nullable =
  ClassName.bestGuess("play.util.function.IntToObjFunction").plusParameter(E.copy(true))
val IntToObjFunction_Any_nullable =
  ClassName.bestGuess("play.util.function.IntToObjFunction").plusParameter(ANY.copy(true))
val LongToObjFunction_E = ClassName.bestGuess("play.util.function.LongToObjFunction").plusParameter(E)
val LongToObjFunction_E_nullable =
  ClassName.bestGuess("play.util.function.LongToObjFunction").plusParameter(E.copy(true))
val LongToObjFunction_Any_nullable =
  ClassName.bestGuess("play.util.function.LongToObjFunction").plusParameter(ANY.copy(true))
val Function_ID_E = LambdaTypeName.get(parameters = arrayOf(ID), returnType = E)
val Function_ID_E_nullable = LambdaTypeName.get(parameters = arrayOf(ID), returnType = E.copy(true))
val Function_Any_Any_nullable = LambdaTypeName.get(parameters = arrayOf(ANY), returnType = ANY.copy(true))

val Function_Int_E = LambdaTypeName.get(parameters = arrayOf(INT), returnType = E)

val Function_Long_E = LambdaTypeName.get(parameters = arrayOf(LONG), returnType = E)

val Function_Unit = LambdaTypeName.get(returnType = UNIT)

val EntityExistsException = ClassName.bestGuess("play.db.cache.EntityExistsException")

val getOrNull = MemberName("play.util", "getOrNull")

val toOptional = MemberName("play.util", "toOptional")

val ExpireEvaluator = ClassName.bestGuess("play.db.cache.ExpireEvaluator")
val DefaultExpireEvaluator = ClassName.bestGuess("play.db.cache.DefaultExpireEvaluator")

val Json = ClassName.bestGuess("play.util.json.Json")

val Future = ClassName.bestGuess("play.util.concurrent.Future")

val Logger = ClassName.bestGuess("mu.KLogger")

val getLogger = MemberName("play", "getLogger")

val getCause = MemberName("play.util.control", "getCause")

val filterNotNull = MemberName("play.util.collection", "filterNotNull")
