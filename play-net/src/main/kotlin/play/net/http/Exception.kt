package play.net.http

import play.util.exception.NoStackTraceException

abstract class HttpRequestParameterException(message: String, cause: Throwable? = null) :
  NoStackTraceException(message, cause)

class ParameterMissionException(name: String) : HttpRequestParameterException(name)

class ParameterFormatException(name: String, cause: Throwable?) : HttpRequestParameterException(name, cause)
