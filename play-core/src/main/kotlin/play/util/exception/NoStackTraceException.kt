package play.util.exception

/**
 * Created by LiangZengle on 2020/2/20.
 */
open class NoStackTraceException(message: String, cause: Throwable? = null) :
  RuntimeException(message, cause, false, false) {
  final override fun fillInStackTrace(): Throwable = this
}
