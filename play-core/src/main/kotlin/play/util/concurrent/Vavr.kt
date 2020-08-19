package play.util.concurrent

import io.vavr.concurrent.Future
import io.vavr.control.Try
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.milliseconds

fun <T> Future<T>.awaitResult(millis: Int): Try<T> {
  return awaitResult(millis.milliseconds)
}

fun <T> Future<T>.awaitResult(duration: java.time.Duration): Try<T> {
  return awaitResult(duration.toMillis().milliseconds)
}

fun <T> Future<T>.awaitResult(duration: Duration): Try<T> {
  await(duration.toLong(TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS)
  return value.get()
}

fun <T> Future<T>.awaitSuccessOrThrow(millis: Int): T {
  return awaitSuccessOrThrow(millis.milliseconds)
}

fun <T> Future<T>.awaitSuccessOrThrow(duration: java.time.Duration): T {
  return awaitSuccessOrThrow(duration.toMillis().milliseconds)
}

fun <T> Future<T>.awaitSuccessOrThrow(duration: Duration): T {
  await(duration.toLong(TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS)
  val tryResult = value.get()
  if (tryResult.isFailure) {
    throw tryResult.cause
  }
  return tryResult.get()
}
