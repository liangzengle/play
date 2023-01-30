package play.scala

import play.util.concurrent.PlayFuture
import scala.compat.java8.FutureConverters
import scala.concurrent.`Await$`
import scala.concurrent.Future
import scala.concurrent.Promise
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage

typealias ScalaFuture<T> = Future<T>

typealias ScalaPromise<T> = Promise<T>

fun <T> scalaPromise(): ScalaPromise<T> = Promise.apply()

fun <T> CompletionStage<T>.toScala(): ScalaFuture<T> = FutureConverters.toScala(this)

fun <T> PlayFuture<T>.toScala(): ScalaFuture<T> = FutureConverters.toScala(toJava())

fun <T> ScalaFuture<T>.toJava(): CompletionStage<T> = FutureConverters.toJava(this)

fun <T> ScalaFuture<T>.toPlay(): PlayFuture<T> = play.util.concurrent.Future(toJava() as CompletableFuture<T>)

fun <T> ScalaFuture<T>.await(timeout: Duration) =
  `Await$`.`MODULE$`.ready(this, timeout.toScala())

fun <T> ScalaFuture<T>.get(timeout: Duration): T =
  `Await$`.`MODULE$`.result(this, timeout.toScala())
