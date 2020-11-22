package play.scala

import scala.util.Failure
import scala.util.Try

fun <T> Try<T>.toResult(): Result<T> = if (this is Failure) Result.failure(exception()) else Result.success(this.get())
