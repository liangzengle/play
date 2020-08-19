package play.inject.guice

import com.google.inject.Guice
import com.google.inject.Module
import com.google.inject.Stage
import play.Application
import play.ApplicationLoader
import play.Log
import java.util.*

/**
 * Created by LiangZengle on 2020/2/20.
 */
class GuiceApplicationLoader : ApplicationLoader {

  override fun load(ctx: ApplicationLoader.Context): Application {
    val modules = ServiceLoader.load(Module::class.java)
    val exclusiveModules = ctx.conf.getStringList("guice.modules.disabled").toSet()
    if (exclusiveModules.isNotEmpty()) {
      Log.info { "Guice ruling out modules: $exclusiveModules" }
    }
    for (module in modules) {
      if (exclusiveModules.contains(module.javaClass.name)) {
        continue
      }
      if (module is GuiceModule) {
        module.initContext(ctx)
      }
    }

    val stage = when (ctx.conf.getString("guice.stage").toLowerCase()) {
      "prod" -> Stage.PRODUCTION
      else -> Stage.DEVELOPMENT
    }

    Log.info { "Guice is running on $stage mode" }
    val injector = Guice.createInjector(stage, modules)
    return injector.getInstance(Application::class.java)
  }
}
