package play.util.logging

abstract class WithLogger {
  protected val logger = PlayLoggerFactory.getLogger(this::class)
}
