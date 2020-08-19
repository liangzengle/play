package play.config.inject

import com.google.auto.service.AutoService
import com.google.inject.Module
import play.Configuration
import play.config.ConfigManager
import play.config.ConfigReader
import play.config.ConfigResolver
import play.config.JsonConfigReader
import play.inject.guice.GuiceModule

/**
 * @author LiangZengle
 */
@AutoService(Module::class)
class ConfigGuiceModule : GuiceModule() {

  override fun configure() {
    val conf = ctx.conf.getConfiguration("config")
    val configResolver = ConfigResolver.forPath(conf.getString("path"))
    bind<Configuration>().qualifiedWith("config").toInstance(conf)
    bind<ConfigResolver>().toInstance(configResolver)
    optionalBind<ConfigReader>().defaultTo<JsonConfigReader>()
    bind<ConfigManager>().asEagerSingleton()
  }
}
