package play.util.http

import play.util.exception.NoStackTraceException

/**
 * http请求返回失败的状态码
 *
 * @property statusCode 状态码
 */
class UnsuccessfulStatusCodeException(val statusCode: Int) : NoStackTraceException("$statusCode")
