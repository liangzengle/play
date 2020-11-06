package play.util.concurrent

import scala.compat.java8.FutureConverters
import scala.concurrent.Future
import scala.concurrent.Promise
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage

typealias ScalaFuture<T> = Future<T>

typealias ScalaPromise<T> = Promise<T>

fun <T> CompletionStage<T>.toScala(): ScalaFuture<T> = FutureConverters.toScala(this)

fun <T> PlayFuture<T>.toScala(): ScalaFuture<T> = FutureConverters.toScala(toCompletableFuture())

fun <T> ScalaFuture<T>.toJava(): CompletionStage<T> = FutureConverters.toJava(this)

fun <T> ScalaFuture<T>.toPlay(): PlayFuture<T> = Future(toJava() as CompletableFuture<T>)
