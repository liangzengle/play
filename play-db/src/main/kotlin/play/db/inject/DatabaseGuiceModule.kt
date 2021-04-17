package play.db.inject

import com.google.auto.service.AutoService
import com.google.inject.Module
import com.google.inject.Provides
import com.typesafe.config.Config
import javax.inject.Named
import javax.inject.Singleton
import play.db.*
import play.db.memory.MemoryRepository
import play.entity.cache.EntityCacheLoader
import play.entity.cache.EntityCacheWriter
import play.inject.guice.GuiceModule

/**
 *
 * @author LiangZengle
 */
@AutoService(Module::class)
class DatabaseGuiceModule : GuiceModule() {
  override fun configure() {
    bindDefault<TableNameFormatter, LowerUnderscoreFormatter>()
    bindToBinding<QueryService>(binding<Repository>())
    bindToBinding<PersistService>(binding<Repository>())
    bindToBinding<EntityCacheLoader>(binding<QueryService>())
    bindToBinding<EntityCacheWriter>(binding<PersistService>())
    bindDefault<Repository, MemoryRepository>()
  }

  @Provides
  @Singleton
  @Named("db")
  fun config(config: Config): Config {
    return config.getConfig("db")
  }

  @Provides
  @Singleton
  fun tableNameResolver(@Named("db") config: Config, tableNameFormatter: TableNameFormatter): TableNameResolver {
    return TableNameResolver(config.getStringList("table-name-trim-postfixes"), tableNameFormatter)
  }
}
