package play.util.concurrent

import play.util.unsafeCast
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.time.Duration
import kotlin.time.milliseconds

typealias PlayFuture<T> = Future<T>

/**
 * A wrapper of CompletableFuture
 * @author LiangZengle
 */
class Future<T>(private val cf: CompletableFuture<T>, private val ec: Executor) {

  constructor(cf: CompletableFuture<T>) : this(cf, cf.defaultExecutor())

  fun toCompletableFuture(): CompletableFuture<T> = cf

  fun <U> map(f: (T) -> U): Future<U> {
    return Future(cf.thenApplyAsync(f, ec))
  }

  fun <U> flatMap(f: (T) -> Future<U>): Future<U> {
    return Future(cf.thenComposeAsync({ f(it).toCompletableFuture() }, ec))
  }

  fun filter(predicate: (T) -> Boolean): Future<T> {
    return flatMap { if (predicate(it)) successful(it) else failed(NoSuchElementException("Predicate failed")) }
  }

  fun mapToFailure(predicate: (T) -> Boolean, mapper: (T) -> Throwable): Future<T> {
    return flatMap { if (predicate(it)) failed(mapper(it)) else successful(it) }
  }

  fun filterNot(predicate: (T) -> Boolean): Future<T> {
    return flatMap { if (!predicate(it)) successful(it) else failed(NoSuchElementException("Predicate failed")) }
  }

  fun andThen(f: (T, Throwable) -> Unit): Future<T> {
    return Future(cf.whenCompleteAsync({ v, e -> f(v, e) }, ec))
  }

  fun onComplete(f: (Result<T>) -> Unit) {
    cf.whenCompleteAsync(
      { v, e ->
        val result = if (e != null) Result.failure(e) else Result.success(v)
        f(result)
      },
      ec
    )
  }

  fun onComplete(onSuccess: (T) -> Unit, onFailure: (Throwable) -> Unit) {
    cf.whenCompleteAsync(
      { v, e ->
        if (e != null) onFailure(e.cause ?: e)
        else onSuccess(v)
      },
      ec
    )
  }

  fun onSuccess(f: (T) -> Unit): Future<T> {
    return Future(cf.whenCompleteAsync({ v, e -> if (e == null) f(v) }, ec))
  }

  fun onFailure(f: (Throwable) -> Unit): Future<T> {
    return Future(cf.whenCompleteAsync({ _, e -> if (e != null) f(e.cause ?: e) }, ec))
  }

  fun recover(f: (Throwable) -> T): Future<T> {
    return Future(cf.exceptionally(f))
  }

  fun <E : Throwable> recover(exceptionType: Class<E>, f: (Throwable) -> T): Future<T> {
    val newCF = CompletableFuture<T>()
    onComplete(
      { newCF.complete(it) },
      { e ->
        if (!exceptionType.isAssignableFrom(e.javaClass)) {
          newCF.completeExceptionally(e)
        } else {
          @Suppress("UNCHECKED_CAST")
          newCF.completeAsync({ f(e as E) }, ec)
        }
      }
    )
    return Future(newCF)
  }

  fun recoverWith(f: (Throwable) -> Future<T>): Future<T> {
    val newCF = CompletableFuture<T>()
    onComplete(
      { newCF.complete(it) },
      { e ->
        f(e).onComplete(
          {
            newCF.complete(it)
          },
          {
            newCF.completeExceptionally(it)
          }
        )
      }
    )
    return Future(newCF)
  }

  fun <E : Throwable> recoverWith(exceptionType: Class<E>, f: (E) -> Future<T>): Future<T> {
    val newCF = CompletableFuture<T>()
    onComplete(
      { newCF.complete(it) },
      { e ->
        if (!exceptionType.isAssignableFrom(e.javaClass)) {
          newCF.completeExceptionally(e)
        } else {
          @Suppress("UNCHECKED_CAST")
          f(e as E).onComplete(
            {
              newCF.complete(it)
            },
            {
              newCF.completeExceptionally(it)
            }
          )
        }
      }
    )
    return Future(newCF)
  }

  fun get(): T {
    return cf.get()
  }

  @Throws(TimeoutException::class)
  fun get(timeout: Duration): T {
    return cf.get(timeout.toLongMilliseconds(), TimeUnit.MILLISECONDS)
  }

  @Throws(TimeoutException::class)
  fun get(timeout: java.time.Duration): T {
    return cf.get(timeout.toMillis(), TimeUnit.MILLISECONDS)
  }

  @Throws(TimeoutException::class)
  fun await(timeout: Duration) {
    cf.get(timeout.toLongMilliseconds(), TimeUnit.MILLISECONDS)
  }

  @Throws(TimeoutException::class)
  fun await(timeout: java.time.Duration) {
    cf.get(timeout.toMillis(), TimeUnit.MILLISECONDS)
  }

  fun getResult(timeout: Duration): Result<T> {
    return try {
      val value = cf.get(timeout.toLongMilliseconds(), TimeUnit.MILLISECONDS)
      Result.success(value)
    } catch (e: Exception) {
      Result.failure(e.cause ?: e)
    }
  }

  fun getResult(timeout: java.time.Duration): Result<T> {
    return getResult(timeout.toMillis().milliseconds)
  }

  fun isCompleted() = cf.isDone

  fun isSuccess() = cf.isDone && !cf.isCompletedExceptionally

  fun isFailed() = cf.isCompletedExceptionally

  companion object {
    @JvmStatic
    fun <T> of(f: () -> T): Future<T> = Future(f)

    @JvmStatic
    fun <T> fromJava(future: java.util.concurrent.Future<T>, executor: Executor): Future<T> {
      return Future(executor, future::get)
    }

    @JvmStatic
    fun <T> fromJava(future: java.util.concurrent.Future<T>): Future<T> {
      return Future(future::get)
    }

    @Suppress("NOTHING_TO_INLINE")
    inline fun <T> CompletableFuture<T>.toFuture(): Future<T> = Future(this)

    operator fun <T> invoke(executor: Executor, f: () -> T): Future<T> {
      return Future(CompletableFuture.supplyAsync(f, executor))
    }

    operator fun <T> invoke(f: () -> T): Future<T> {
      return Future(CompletableFuture.supplyAsync(f))
    }

    @JvmStatic
    fun <T> failed(cause: Throwable): Future<T> {
      return Future(CompletableFuture.failedFuture(cause))
    }

    @JvmStatic
    fun <T> successful(value: T): Future<T> {
      return Future(CompletableFuture.completedFuture(value))
    }

    @JvmStatic
    fun <T> firstOf(futures: Collection<Future<out T>>): Future<out T> {
      val futureArrays = arrayOfNulls<CompletableFuture<out T>>(futures.size)
      futures.forEachIndexed { i, f ->
        futureArrays[i] = f.cf
      }
      val firstCompleted = CompletableFuture.anyOf(*futureArrays)
      return Future(firstCompleted.unsafeCast())
    }

    @JvmStatic
    fun allOf(futures: Collection<Future<*>>): Future<Void> {
      val futureArrays = arrayOfNulls<CompletableFuture<*>>(futures.size)
      futures.forEachIndexed { i, f ->
        futureArrays[i] = f.cf
      }
      return Future(CompletableFuture.allOf(*futureArrays))
    }
  }
}
