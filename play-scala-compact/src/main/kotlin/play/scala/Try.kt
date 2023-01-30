package play.scala

import play.util.control.getCause
import scala.util.Failure
import scala.util.Success
import scala.util.Try

fun <T> scalaTry(r: () -> T): Try<T> = Try.apply(r)

fun <T> Try<T>.toResult(): Result<T> = when (this) {
  is Success<T> -> Result.success(this.get())
  is Failure<T> -> Result.failure(this.exception())
  else -> error("won't happen")
}

fun <T> Result<T>.toTry(): Try<T> = if (isSuccess) Success(getOrNull()) else Failure(getCause())
