package play.entity.inject

import com.google.auto.service.AutoService
import com.google.inject.Module
import com.google.inject.Provides
import com.typesafe.config.Config
import javax.inject.Named
import javax.inject.Singleton
import play.entity.cache.EntityCacheFactory
import play.entity.cache.EntityCacheManager
import play.entity.cache.chm.CHMEntityCacheFactory
import play.inject.guice.GuiceModule

/**
 * Entity Cache Module
 *
 * @author LiangZengle
 */
@AutoService(Module::class)
class EntityCacheGuiceModule : GuiceModule() {
  override fun configure() {
    bindSingleton<EntityCacheManager>()
    bindDefault<EntityCacheFactory, CHMEntityCacheFactory>()
  }

  @Provides
  @Singleton
  @Named("entity")
  fun config(config: Config): Config {
    return config.getConfig("entity")
  }
}
