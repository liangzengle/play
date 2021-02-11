@file:Suppress("NOTHING_TO_INLINE")

package play.scala

import java.util.*
import play.util.control.getCause
import scala.util.Either
import scala.util.Left
import scala.util.Right

inline fun <L, R> right(value: R) = Right.apply<L, R>(value)

inline fun <L, R> left(value: L) = Left.apply<L, R>(value)

fun <T> Result<T>.toEither(): Either<Throwable, T> {
  return if (isSuccess) right(getOrThrow()) else left(getCause())
}

fun <L, R> Either<L, R>.toOptional(): Optional<R> =
  if (this is Right<L, R>) Optional.ofNullable(value()) else Optional.empty()
