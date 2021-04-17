package play.inject.guice

import com.google.auto.service.AutoService
import com.google.inject.Module
import play.inject.guice.GuiceModule

/**
 * Binder Configure Module
 *
 * @author LiangZengle
 */
@AutoService(Module::class)
class BinderConfigureGuiceModule : GuiceModule() {
  override fun configure() {
    binder().disableCircularProxies()
    binder().requireExactBindingAnnotations()
  }
}
