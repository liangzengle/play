package play.entity.cache.codegen

import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import play.entity.cache.*

/**
 *
 * @author LiangZengle
 */
object AddConstructor : EntityCacheComponent() {
  override fun accept(): Boolean {
    return true
  }

  override fun apply() {
    cache.primaryConstructor(
      FunSpec.constructorBuilder()
        .addParameter("entityClass", Class_E)
        .addParameter("persistService", PersistService)
        .addParameter("queryService", QueryService)
        .addParameter("injector", Injector)
        .addParameter("scheduler", Scheduler)
        .addParameter("executor", DbExecutor)
        .addParameter("initializerProvider", initializerProvider)
        .addParameter("conf", EntityCacheSettings)
        .build()
    )
      .addProperty(
        PropertySpec.builder("entityClass", Class_E, KModifier.OVERRIDE)
          .initializer("entityClass")
          .build()
      )
      .addProperty(
        PropertySpec.builder("persistService", PersistService, KModifier.PRIVATE)
          .initializer("persistService")
          .build()
      ).addProperty(
        PropertySpec.builder("queryService", QueryService, KModifier.PRIVATE)
          .initializer("queryService")
          .build()
      ).addProperty(
        PropertySpec.builder("conf", EntityCacheSettings, KModifier.PRIVATE)
          .initializer("conf")
          .build()
      ).addProperty(
        PropertySpec.builder("initializerProvider", initializerProvider, KModifier.PRIVATE)
          .initializer("initializerProvider")
          .build()
      )
  }
}
