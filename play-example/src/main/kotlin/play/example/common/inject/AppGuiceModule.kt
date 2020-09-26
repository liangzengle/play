package play.example.common.inject

import com.google.auto.service.AutoService
import com.google.inject.Module
import play.inject.guice.GuiceModule

@AutoService(Module::class)
class AppGuiceModule : GuiceModule() {

  override fun configure() {
    when (ctx.conf.getString("server.mode")?.toLowerCase()) {
      "local" -> install(LocalGuiceModule())
      "remote" -> install(RemoteGuiceModule())
    }
  }
}
