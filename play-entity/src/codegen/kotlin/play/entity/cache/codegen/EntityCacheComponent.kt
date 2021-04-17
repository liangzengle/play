package play.entity.cache.codegen

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import play.entity.cache.*
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater

abstract class EntityCacheComponent {

  protected lateinit var ctx: CacheContext

  protected val cache get() = ctx.cache

  fun apply(t: CacheContext) {
    ctx = t
    if (accept()) {
      apply()
    }
  }

  abstract fun accept(): Boolean

  abstract fun apply()

  protected fun getIdType(): TypeName {
    return when (ctx.idType) {
      INT -> INT
      LONG -> LONG
      else -> ID_Any
    }
  }

  protected fun isPrimitiveId(): Boolean {
    return when (ctx.idType) {
      INT -> true
      LONG -> true
      else -> false
    }
  }

  protected fun getCreationType(): TypeName {
    return when (ctx.idType) {
      INT -> IntToObjFunction_E
      LONG -> LongToObjFunction_E
      else -> Function_ID_E
    }
  }

  protected fun getDeletedSetType(): TypeName {
    return when (ctx.idType) {
      INT -> NonBlockingHashSetInt
      LONG -> NonBlockingHashSetLong
      else -> ConcurrentHashSet
    }
  }

  protected fun getClassTypeVariableNames(): List<TypeVariableName> {
    return when (ctx.idType) {
      INT -> listOf(EntityInt_E)
      LONG -> listOf(EntityLong_E)
      else -> listOf(ID_Any, E_Entity_ID)
    }
  }

  protected fun getId(): String {
    return when (ctx.idType) {
      INT -> "id"
      LONG -> "id"
      else -> "id()"
    }
  }

  protected fun getLoaderType(): TypeName {
    return when (ctx.idType) {
      INT -> IntToObjFunction_E_nullable
      LONG -> LongToObjFunction_E_nullable
      else -> Function_ID_E_nullable
    }
  }

  protected fun getExpiredUpdaterType(): TypeName {
    return when (ctx.idType) {
      INT ->
        AtomicIntegerFieldUpdater::class.asClassName()
          .parameterizedBy(ClassName.bestGuess("CacheObj").parameterizedBy(STAR))
      LONG ->
        AtomicIntegerFieldUpdater::class.asClassName()
          .parameterizedBy(ClassName.bestGuess("CacheObj").parameterizedBy(STAR))
      else ->
        AtomicIntegerFieldUpdater::class.asClassName()
          .parameterizedBy(ClassName.bestGuess("CacheObj").parameterizedBy(STAR, STAR))
    }
  }
}
