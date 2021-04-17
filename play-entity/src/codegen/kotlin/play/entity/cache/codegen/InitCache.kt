package play.entity.cache.codegen

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.plusParameter
import play.entity.cache.*

/**
 *
 * @author LiangZengle
 */
object InitCache : EntityCacheComponent() {
  override fun apply() {
    ctx.cache = when (ctx.idType) {
      INT -> {
        TypeSpec.classBuilder(ctx.className)
          .addTypeVariable(EntityInt_E)
          .addSuperinterface(EntityCacheInt.plusParameter(EntityInt_E))
          .addSuperinterface(EntityCache.parameterizedBy(INT, E))
      }
      LONG -> {
        TypeSpec.classBuilder(ctx.className)
          .addTypeVariable(EntityLong_E)
          .addSuperinterface(EntityCacheLong.plusParameter(EntityLong_E))
          .addSuperinterface(EntityCache.parameterizedBy(LONG, E))
      }
      else -> {
        TypeSpec.classBuilder(ctx.className).addTypeVariable(ID_Any).addTypeVariable(E_Entity_ID)
          .addSuperinterface(EntityCache.parameterizedBy(ID_Any, E_Entity_ID))
      }
    }

    cache.addModifiers(KModifier.INTERNAL)

    cache.addType(
      TypeSpec.companionObjectBuilder()
        .addProperty(createLogger())
        .build()
    )

    cache.addAnnotation(
      AnnotationSpec.builder(Suppress::class)
        .addMember("%S", "RedundantVisibilityModifier")
        .addMember("%S", "RedundantUnitReturnType")
        .build()
    )
  }

  private fun createLogger(): PropertySpec {
    return PropertySpec.builder("logger", Logger)
      .addAnnotation(JvmStatic::class)
      .initializer("%M()", getLogger)
      .build()
  }

  override fun accept(): Boolean {
    return true
  }
}
