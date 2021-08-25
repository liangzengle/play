package play.scala

import scala.util.Failure
import scala.util.Success
import scala.util.Try

fun <T> Try<T>.toResult(): Result<T> = when (this) {
  is Success<T> -> Result.success(this.get())
  is Failure<T> -> Result.failure(this.exception())
  else -> error("won't happen")
}
