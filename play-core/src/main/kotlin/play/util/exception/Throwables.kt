@file:JvmName("ThrowableUtil")

package play.util.exception

import java.io.IOException
import java.io.UncheckedIOException
import java.lang.reflect.UndeclaredThrowableException

fun Throwable.isFatal(): Boolean = when (this) {
  is VirtualMachineError -> true
  is ThreadDeath -> true
  is InterruptedException -> true
  is LinkageError -> true
  else -> false
}

fun Throwable.isNotFatal(): Boolean = !isFatal()

fun Throwable.rethrow() {
  if (this is RuntimeException) {
    throw this
  }
  if (this is Error) {
    throw this
  }
  if (this is IOException) {
    throw UncheckedIOException(this)
  }
  throw UndeclaredThrowableException(this)
}
