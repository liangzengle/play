package play.db

import play.ApplicationLifecycle
import play.Log
import java.io.Closeable

abstract class Repository(lifecycle: ApplicationLifecycle) : PersistService, QueryService, Closeable {
  init {
    Log.info { "Using ${javaClass.simpleName}" }
    lifecycle.addShutdownHook("Shutdown [${javaClass.simpleName}]", ApplicationLifecycle.PRIORITY_LOWEST) {
      close()
    }
  }
}
