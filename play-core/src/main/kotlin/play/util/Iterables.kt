package play.util

import java.util.stream.Stream
import java.util.stream.StreamSupport

fun <T> Iterable<T>.mkStringTo(
  b: StringBuilder,
  separator: Char,
  transform: ((T) -> String)? = null
): String {
  var first = true
  for (e in this) {
    if (!first) {
      b.append(separator)
    }
    if (transform == null) b.append(e) else b.append(transform(e))
    first = false
  }
  return b.toString()
}

fun <T> Iterable<T>.mkString(
  separator: Char,
  transform: ((T) -> String)? = null
): String {
  val b = StringBuilder()
  var first = true
  for (e in this) {
    if (!first) {
      b.append(separator)
    }
    if (transform == null) b.append(e) else b.append(transform(e))
    first = false
  }
  return b.toString()
}

fun <T> Iterable<T>.mkString(
  separator: Char,
  prefix: Char,
  postfix: Char,
  transform: ((T) -> String)? = null
): String {
  val b = StringBuilder()
  b.append(prefix)
  var first = true
  for (e in this) {
    if (!first) b.append(separator)
    if (transform == null) b.append(e) else b.append(transform(e))
    first = false
  }
  b.append(postfix)
  return b.toString()
}

fun <E> Iterable<E>.toStream(): Stream<E> {
  return if( this is Collection<E>) stream() else StreamSupport.stream(this.spliterator(), false)
}
