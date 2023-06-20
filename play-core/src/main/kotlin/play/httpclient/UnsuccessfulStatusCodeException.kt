package play.httpclient

/**
 * http请求返回失败的状态码
 *
 * @property uri 请求的uri
 * @property statusCode 状态码
 */
class UnsuccessfulStatusCodeException(val uri: String, val statusCode: Int) :
  RuntimeException("$uri $statusCode", null, false, false)
