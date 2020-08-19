package play.net.http

abstract class HttpRequestFilter {
  abstract fun accept(request: BasicHttpRequest): Boolean
}
