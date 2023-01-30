package play.scala

import play.util.control.getCause
import scala.util.Either
import scala.util.Left
import scala.util.Right
import java.util.*

fun <L, R> scalaRight(value: R) = Right.apply<L, R>(value)

fun <L, R> scalaLeft(value: L) = Left.apply<L, R>(value)

fun <T> Result<T>.toEither(): Either<Throwable, T> {
  return if (isSuccess) scalaRight(getOrThrow()) else scalaLeft(getCause())
}

fun <L, R : Any> Either<L, R>.getRight(): Optional<R> =
  if (this is Right<L, R>) Optional.ofNullable(value()) else Optional.empty()

fun <L : Any, R> Either<L, R>.getLeft(): Optional<L> =
  if (this is Left<L, R>) Optional.ofNullable(value()) else Optional.empty()
