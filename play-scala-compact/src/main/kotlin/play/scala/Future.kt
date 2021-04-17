package play.scala

import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import play.scala.collection.Scala
import play.util.concurrent.PlayFuture
import scala.compat.java8.FutureConverters
import scala.concurrent.Future
import scala.concurrent.Promise

typealias ScalaFuture<T> = Future<T>

typealias ScalaPromise<T> = Promise<T>

fun <T> CompletionStage<T>.toScala(): ScalaFuture<T> = FutureConverters.toScala(this)

fun <T> PlayFuture<T>.toScala(): ScalaFuture<T> = FutureConverters.toScala(toJava())

fun <T> ScalaFuture<T>.toJava(): CompletionStage<T> = FutureConverters.toJava(this)

fun <T> ScalaFuture<T>.toPlay(): PlayFuture<T> = play.util.concurrent.Future(toJava() as CompletableFuture<T>)

fun <T> promise(): ScalaPromise<T> = Scala.promise()

fun <T> ScalaFuture<T>.await(timeout: Duration) = Scala.await(this, timeout.toMillis())

fun <T> ScalaFuture<T>.get(timeout: Duration): T = Scala.get(this, timeout.toMillis())
