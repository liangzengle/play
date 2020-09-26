package play.db.cache

import play.Configuration
import play.inject.guice.GuiceModule

internal class EntityCacheGuiceModule : GuiceModule() {
  override fun configure() {
    super.configure()
    val cacheConf = ctx.conf.getConfiguration("db.cache")
    bind<Configuration>().qualifiedWith("cache").toInstance(cacheConf)
    optionalBind<EntityCacheFactory>().defaultTo<DefaultEntityCacheFactory>()
  }
}
