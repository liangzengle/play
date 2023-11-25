package play

import play.util.logging.PlayLogger
import play.util.logging.PlayLoggerFactory

object Application : PlayLogger by PlayLoggerFactory.getLogger("application") {
  fun pid() = ProcessHandle.current().pid()
}
