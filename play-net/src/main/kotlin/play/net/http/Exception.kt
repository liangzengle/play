package play.net.http

import play.util.exception.NoStackTraceException
import java.lang.reflect.Method
import java.lang.reflect.Type

abstract class HttpRequestParameterException(message: String, cause: Throwable? = null) :
  NoStackTraceException(message, cause)

class ParameterMissionException(name: String) : HttpRequestParameterException(name)

class ParameterFormatException(name: String, cause: Throwable?) : HttpRequestParameterException(name, cause)

class UnsupportedHttpParameterTypeException(method: Method, type: Type) :
  HttpRequestParameterException("$type in $method")

class UnsuccessfulStatusCodeException(val statusCode: Int) : NoStackTraceException("$statusCode")
