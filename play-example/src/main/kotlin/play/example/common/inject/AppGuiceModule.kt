package play.example.common.inject

import com.google.auto.service.AutoService
import com.google.inject.Module
import play.inject.guice.GuiceModule

@AutoService(Module::class)
class AppGuiceModule : GuiceModule() {

  override fun configure() {
    val module = when (ctx.conf.getString("server.mode")?.toLowerCase()) {
      "local" -> LocalGuiceModule()
      "remote" -> RemoteGuiceModule()
      else -> return
    }
    module.initContext(ctx)
    install(module)
  }
}
