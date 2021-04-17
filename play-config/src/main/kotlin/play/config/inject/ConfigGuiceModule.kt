package play.config.inject

import com.google.auto.service.AutoService
import com.google.inject.Module
import com.google.inject.Provides
import com.google.inject.Singleton
import com.typesafe.config.Config
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider
import play.config.*
import play.inject.PlayInjector
import play.inject.guice.GuiceModule

/**
 * Config Module
 *
 * @author LiangZengle
 */
@AutoService(Module::class)
class ConfigGuiceModule : GuiceModule() {
  override fun configure() {
    bindEagerlySingleton<ConfigManager>()
    bindSingleton<ResourceReader>()
    bindDefaultProvider<ConfigReader, ConfigReaderProvider>()
  }

  @Provides
  @Singleton
  @Named("config")
  fun config(config: Config): Config {
    return config.getConfig("config")
  }

  @Provides
  @Singleton
  fun configResolver(@Named("config") config: Config): ConfigResolver {
    return ConfigResolver.forPath(config.getString("path"))
  }

  @Provides
  @Singleton
  fun configReloadListeners(injector: PlayInjector): List<@JvmWildcard ConfigReloadListener> {
    return injector.getInstancesOfType(ConfigReloadListener::class.java)
  }

  @Provides
  @Singleton
  fun configValidators(injector: PlayInjector): List<@JvmWildcard ConfigValidator> {
    return injector.getInstancesOfType(ConfigValidator::class.java)
  }
}

class ConfigReaderProvider @Inject constructor(resolver: ConfigResolver) : Provider<ConfigReader> {

  private val value by lazy {
    JsonConfigReader(resolver)
  }

  override fun get(): ConfigReader = value

}
