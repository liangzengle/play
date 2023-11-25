package play.httpclient

import play.util.concurrent.PlayFuture
import java.time.Duration

abstract class Get {
  protected var uri = ""
  protected var timeout: Duration = PlayHttpClient.DEFAULT_READ_TIMEOUT
  protected val headers = ArrayList<NameValue>()
  protected val params = ArrayList<NameValue>()

  fun uri(uri: String): Get {
    this.uri = uri
    return this
  }

  fun timeout(timeout: Duration): Get {
    this.timeout = timeout
    return this
  }

  fun header(name: String, value: String): Get {
    headers.add(NameValue(name, value))
    return this
  }

  fun headers(headers: Map<String, String>): Get {
    headers.forEach(::header)
    return this
  }

  fun param(name: String, value: String): Get {
    params.add(NameValue(name, value))
    return this
  }

  fun params(params: Map<String, String>): Get {
    params.forEach(::param)
    return this
  }

  abstract fun sendAsync(): PlayFuture<String>
}

abstract class Post {
  protected var uri = ""
  protected var timeout: Duration = PlayHttpClient.DEFAULT_READ_TIMEOUT
  protected val headers = ArrayList<NameValue>()
  protected val form = ArrayList<NameValue>()
  protected var body: String? = null

  fun uri(uri: String): Post {
    this.uri = uri
    return this
  }

  fun timeout(timeout: Duration): Post {
    this.timeout = timeout
    return this
  }

  fun header(name: String, value: String): Post {
    headers.add(NameValue(name, value))
    return this
  }

  fun headers(headers: Map<String, String>): Post {
    headers.forEach(::header)
    return this
  }

  fun formParam(name: String, value: String): Post {
    form.add(NameValue(name, value))
    return this
  }

  fun formParams(params: Map<String, String>): Post {
    params.forEach(::formParam)
    return this
  }

  fun body(body: String): Post {
    this.body = body
    return this;
  }

  abstract fun sendAsync(): PlayFuture<String>
}
