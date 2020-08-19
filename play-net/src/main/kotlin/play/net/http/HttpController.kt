package play.net.http

/**
 *
 */
@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.RUNTIME)
annotation class HttpController(val value: String)
