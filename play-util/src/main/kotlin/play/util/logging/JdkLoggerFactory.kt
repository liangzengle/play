package play.util.logging

object JdkLoggerFactory : PlayLoggerFactory {
  override fun getLogger(name: String): PlayLogger = JdkLogger(System.getLogger(name))
}
