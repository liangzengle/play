package play.db.cache.codegen

import com.squareup.kotlinpoet.*
import play.db.cache.*

/**
 *
 * @author LiangZengle
 */
object AddProperties : EntityCacheComponent() {
  override fun apply() {
    when (ctx.idType) {
      INT -> {
        cache
          .addProperty(
            PropertySpec.builder("cache", ConcurrentIntObjectMap_CacheObj_ID_E, KModifier.PRIVATE)
              .initializer(
                "%T(%T.getInitialSizeOrDefault(entityClass, conf.initialSize))",
                ConcurrentIntObjectMap,
                EntityCacheHelper
              ).build()
          )
          .addProperty(
            PropertySpec.builder("persistingEntities", ConcurrentIntObjectMap_E, KModifier.PRIVATE)
              .initializer("%T()", ConcurrentIntObjectMap)
              .build()
          )
          .addProperty(
            PropertySpec.builder("deleted", NonBlockingHashSetInt.copy(true), KModifier.PRIVATE)
              .mutable(true)
              .initializer("null")
              .addAnnotation(Volatile::class)
              .build()
          )
      }
      LONG -> {
        cache
          .addProperty(
            PropertySpec.builder("cache", ConcurrentLongObjectMap_CacheObj_ID_E, KModifier.PRIVATE)
              .initializer(
                "%T(%T.getInitialSizeOrDefault(entityClass, conf.initialSize))",
                ConcurrentLongObjectMap,
                EntityCacheHelper
              ).build()
          )
          .addProperty(
            PropertySpec.builder("persistingEntities", ConcurrentLongObjectMap_E, KModifier.PRIVATE)
              .initializer("%T()", ConcurrentLongObjectMap)
              .build()
          )
          .addProperty(
            PropertySpec.builder("deleted", NonBlockingHashSetLong.copy(true), KModifier.PRIVATE)
              .mutable(true)
              .initializer("null")
              .addAnnotation(Volatile::class)
              .build()
          )
      }
      else -> {
        cache
          .addProperty(
            PropertySpec.builder("cache", ConcurrentMap_ID_CacheObj_ID_E, KModifier.PRIVATE)
              .initializer(
                "%T(%T.getInitialSizeOrDefault(entityClass, conf.initialSize))",
                ConcurrentHashMap,
                EntityCacheHelper
              ).build()
          )
          .addProperty(
            PropertySpec.builder("persistingEntities", ConcurrentMap_ID_E, KModifier.PRIVATE)
              .initializer("%T()", ConcurrentHashMap)
              .build()
          )
          .addProperty(
            PropertySpec.builder("deleted", MutableSet_ID.copy(true), KModifier.PRIVATE)
              .mutable(true)
              .initializer("null")
              .addAnnotation(Volatile::class)
              .build()
          )
      }
    }

    cache.addProperty("expireEvaluator", ExpireEvaluator, KModifier.PRIVATE)

    cache.addProperty(
      PropertySpec.builder("dbLoader", getDbLoaderType())
        .initializer(
          CodeBlock.builder()
            .addStatement("${getDbLoaderLambdaType()}{ id ->")
            .indent()
            .addStatement("val pendingPersist = persistingEntities.remove(id)")
            .beginControlFlow("if (pendingPersist != null)")
            .addStatement("pendingPersist")
            .nextControlFlow("else")
            .addStatement("val f = queryService.findById(id, entityClass)")
            .beginControlFlow("try")
            .addStatement("val entity: E? = f.get(5.%M).%M()", seconds, getOrNull)
            .beginControlFlow("if (entity != null)")
            .addStatement("entityProcessor.postLoad(entity)")
            .endControlFlow()
            .addStatement("entity")
            .nextControlFlow("catch (e: Exception)")
            .addStatement("""logger.error(e) { "查询数据库失败: ${'$'}{entityClass.simpleName}(${'$'}id)" }""")
            .addStatement("throw e")
            .endControlFlow()
            .endControlFlow()
            .unindent()
            .addStatement("}")
            .build()
        )
        .build()
    )
  }

  private fun getDbLoaderType(): TypeName {
    return when (ctx.idType) {
      INT -> IntToObjFunction_E_nullable
      LONG -> LongToObjFunction_E_nullable
      else -> Function_ID_E_nullable
    }
  }

  private fun getDbLoaderLambdaType(): String {
    return when (ctx.idType) {
      INT -> "IntToObjFunction"
      LONG -> "LongToObjFunction"
      else -> ""
    }
  }

  override fun accept(): Boolean {
    return true
  }
}
