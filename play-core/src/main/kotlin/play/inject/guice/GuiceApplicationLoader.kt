package play.inject.guice

import com.google.common.collect.Iterables
import com.google.inject.Guice
import com.google.inject.Module
import com.google.inject.Stage
import java.util.*
import play.Application
import play.ApplicationLoader
import play.Log

/**
 * Created by LiangZengle on 2020/2/20.
 */
class GuiceApplicationLoader : ApplicationLoader {

  override fun load(ctxt: ApplicationLoader.Context): Application {
    val conf = ctxt.conf
    var modules: Iterable<Module> = Iterables.concat(
      ServiceLoader.load(Module::class.java),
      ServiceLoader.load(GeneratedMultiBindModule::class.java)
    )
    val exclusiveModules = conf.getStringList("guice.modules.disabled").toSet()
    if (exclusiveModules.isNotEmpty()) {
      modules = modules.filterNot { exclusiveModules.contains(it.javaClass.name) }
      Log.info { "Guice ruling out modules: $exclusiveModules" }
    }
    for (module in modules) {
      if (module is ContextualGuiceModule) {
        module.setContext(ctxt)
      }
    }

    val stage = when (conf.getString("guice.stage").toLowerCase()) {
      "prod" -> Stage.PRODUCTION
      else -> Stage.DEVELOPMENT
    }

    Log.info { "Guice is running on $stage mode" }
    val injector = Guice.createInjector(stage, modules)
    return injector.getInstance(Application::class.java)
  }
}
