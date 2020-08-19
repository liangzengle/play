package play.util.http

import play.util.concurrent.Future

interface PlayHttpClient : AutoCloseable {
  fun get(url: String): Future<String> {
    return get(url, mapOf())
  }

  fun get(url: String, params: Map<String, Any>): Future<String> {
    return get(url, mapOf(), mapOf())
  }

  fun get(url: String, params: Map<String, Any>, headers: Map<String, String>): Future<String>

  fun post(url: String): Future<String> {
    return post(url, mapOf())
  }

  fun post(url: String, form: Map<String, Any>): Future<String> {
    return post(url, mapOf(), mapOf())
  }

  fun post(url: String, form: Map<String, Any>, headers: Map<String, String>): Future<String>

  fun post(url: String, data: String): Future<String> {
    return post(url, data, mapOf())
  }

  fun post(url: String, data: String, headers: Map<String, String>): Future<String>
}
