package play.util.logging

abstract class WithLogger {
  protected val logger = PlayLoggerManager.getLogger(this::class)
}
