package play.inject.guice

import play.ApplicationLoader

abstract class ContextualGuiceModule : GuiceModule() {
  protected lateinit var ctxt: ApplicationLoader.Context
  internal fun setContext(ctxt: ApplicationLoader.Context) {
    this.ctxt = ctxt
  }

  protected val conf get() = ctxt.conf

  protected val shutdownCoordinator get() = ctxt.shutdownCoordinator
}
