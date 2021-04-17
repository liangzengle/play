package play

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import play.inject.guice.GuiceApplicationLoader

/**
 *
 * @author LiangZengle
 */
class ApplicationBuilder {
  private var mainConfig: Config? = null
  private var applicationLoader: ApplicationLoader? = null
  private var shutdownCoordinator: ShutdownCoordinator? = null

  internal fun build(): Application {
    val referenceConf = ConfigFactory.parseResources("reference.conf")
    val applicationConf = ConfigFactory.defaultApplication()
    var mainConf = mainConfig?.withFallback(applicationConf) ?: applicationConf
    mainConf = mainConf.withFallback(referenceConf).resolve()
    val conf = mainConf.getConfig("app")
    ModeDependent.setMode(Mode.Dev) // TODO
    val loader = this.applicationLoader ?: GuiceApplicationLoader()
    val shutdownCoordinator = shutdownCoordinator ?: DefaultShutdownCoordinator()
    val ctxt = ApplicationLoader.Context(conf, shutdownCoordinator)
    return loader.load(ctxt)
  }

  fun mainConfig(config: Config): ApplicationBuilder {
    mainConfig = config
    return this
  }

  fun loader(applicationLoader: ApplicationLoader): ApplicationBuilder {
    this.applicationLoader = applicationLoader
    return this
  }

  fun shutdownCoordinator(shutdownCoordinator: ShutdownCoordinator): ApplicationBuilder {
    this.shutdownCoordinator = shutdownCoordinator
    return this
  }
}
