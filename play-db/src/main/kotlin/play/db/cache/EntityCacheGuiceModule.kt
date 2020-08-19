package play.db.cache

import com.google.auto.service.AutoService
import com.google.inject.Module
import play.Configuration
import play.inject.guice.GuiceModule

@AutoService(Module::class)
class EntityCacheGuiceModule : GuiceModule() {
  override fun configure() {
    super.configure()
    val cacheConf = ctx.conf.getConfiguration("db.cache")
    bind<Configuration>().qualifiedWith("cache").toInstance(cacheConf)
    optionalBind<EntityCacheFactory>().defaultTo<DefaultEntityCacheFactory>()
  }
}
