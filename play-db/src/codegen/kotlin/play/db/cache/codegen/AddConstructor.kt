package play.db.cache.codegen

import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import play.db.cache.*

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
        .addParameter("conf", EntityCacheConf)
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
        PropertySpec.builder("conf", EntityCacheConf, KModifier.PRIVATE)
          .initializer("conf")
          .build()
      )
  }
}
