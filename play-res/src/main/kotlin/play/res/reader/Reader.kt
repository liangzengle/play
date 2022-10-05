package play.res.reader

import java.net.URL

interface Reader {

  fun getURL(clazz: Class<*>): Result<URL>

  fun <T> read(clazz: Class<T>): Result<List<T>>
}

