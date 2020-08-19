package play.entity.cache.codegen

import com.squareup.kotlinpoet.*
import play.entity.cache.E_Entity_ID
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater

/**
 *
 * @author LiangZengle
 */
object AddCacheObj : EntityCacheComponent() {
  override fun accept(): Boolean {
    return true
  }

  override fun apply() {
    cache.addType(createCacheObj(getClassTypeVariableNames(), getIdType()))
  }

  private fun createCacheObj(typeVariables: List<TypeVariableName>, idType: TypeName): TypeSpec {
    return TypeSpec.classBuilder("CacheObj").addTypeVariables(typeVariables)
      .addModifiers(KModifier.PRIVATE)
      .primaryConstructor(
        FunSpec.constructorBuilder()
          .addParameter("entity", E_Entity_ID.copy(true))
          .addParameter(
            ParameterSpec.builder("accessTime", LONG)
              .addAnnotation(Volatile::class)
              .build()
          )
          .build()

      )
      .addProperty(
        PropertySpec.builder("entity", E_Entity_ID.copy(true), KModifier.PRIVATE)
          .initializer("entity")
          .build()
      ).addProperty(
        PropertySpec.builder("accessTime", LONG)
          .mutable(true)
          .initializer("accessTime")
          .build()
      ).addProperty(
        PropertySpec.builder("lastPersistTime", LONG)
          .mutable(true)
          .initializer("0L")
          .build()
      ).addProperty(
        PropertySpec.builder("expired", INT)
          .mutable(true)
          .addAnnotation(Volatile::class)
          .initializer("0")
          .build()
      )
      .addFunction(
        FunSpec.builder("isExpired").addStatement("return expired == 1").build()
      )
      .addFunction(
        FunSpec.builder("setExpired")
          .addCode(
            CodeBlock.builder()
              .beginControlFlow("if (!ExpiredUpdater.compareAndSet(this, 0, 1))")
              .addStatement("""throw IllegalStateException("Entity Expired")""")
              .endControlFlow()
              .build()
          )
          .build()
      )
      .addFunction(
        FunSpec.builder("getEntity")
          .returns(E_Entity_ID.copy(true))
          .addStatement("accessTime = currentMillis()")
          .addStatement("return entity")
          .build()
      )
      .addFunction(
        FunSpec.builder("getEntitySilently")
          .returns(E_Entity_ID.copy(true))
          .addStatement("return entity")
          .build()
      )
      .addFunction(
        FunSpec.builder("getId")
          .returns(idType)
          .addStatement("return entity!!.%L", getId())
          .build()
      )
      .addFunction(
        FunSpec.builder("hasEntity")
          .addStatement("return entity != null")
          .build()
      )
      .addFunction(
        FunSpec.builder("isEmpty")
          .addStatement("return entity == null")
          .build()
      )
      .addType(createCompanionObject())
      .build()
  }

  private fun createCompanionObject(): TypeSpec {
    return TypeSpec.companionObjectBuilder()
      .addProperty(
        PropertySpec.builder(
          "ExpiredUpdater",
          getExpiredUpdaterType(),
          KModifier.PRIVATE
        )
          .initializer("""%T.newUpdater(CacheObj::class.java, "expired")""", AtomicIntegerFieldUpdater::class)
          .build()
      ).build()
  }
}
