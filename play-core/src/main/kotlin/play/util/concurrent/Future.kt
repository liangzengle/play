package play.util.concurrent

import play.util.control.getCause
import play.util.unsafeCast
import java.util.concurrent.*
import kotlin.time.Duration
import kotlin.time.toKotlinDuration

typealias PlayFuture<T> = Future<T>

/**
 * A wrapper of CompletableFuture
 * @author LiangZengle
 */
class Future<T>(private val cf: CompletableFuture<T>) {

  fun toJava(): CompletableFuture<T> = cf

  fun <U> map(f: (T) -> U): Future<U> {
    return Future(cf.thenApply(f))
  }

  fun <U> map(ec: Executor, f: (T) -> U): Future<U> {
    return Future(cf.thenApplyAsync(f, ec))
  }

  fun <U> flatMap(f: (T) -> Future<U>): Future<U> {
    return Future(cf.thenCompose { f(it).toJava() })
  }

  fun <U> flatMap(ec: Executor, f: (T) -> Future<U>): Future<U> {
    return Future(cf.thenComposeAsync({ f(it).toJava() }, ec))
  }

  fun filter(predicate: (T) -> Boolean): Future<T> {
    return flatMap { if (predicate(it)) successful(it) else failed(NoSuchElementException("Predicate failed")) }
  }

  fun filter(ec: Executor, predicate: (T) -> Boolean): Future<T> {
    return flatMap(ec) { if (predicate(it)) successful(it) else failed(NoSuchElementException("Predicate failed")) }
  }

  fun mapToFailure(predicate: (T) -> Boolean, mapper: (T) -> Throwable): Future<T> {
    return flatMap { if (predicate(it)) failed(mapper(it)) else successful(it) }
  }

  fun mapToFailure(ec: Executor, predicate: (T) -> Boolean, mapper: (T) -> Throwable): Future<T> {
    return flatMap(ec) { if (predicate(it)) failed(mapper(it)) else successful(it) }
  }

  fun filterNot(predicate: (T) -> Boolean): Future<T> {
    return flatMap { if (!predicate(it)) successful(it) else failed(NoSuchElementException("Predicate failed")) }
  }

  fun filterNot(ec: Executor, predicate: (T) -> Boolean): Future<T> {
    return flatMap(ec) { if (!predicate(it)) successful(it) else failed(NoSuchElementException("Predicate failed")) }
  }

  fun andThen(f: (T, Throwable) -> Unit): Future<T> {
    return Future(cf.whenComplete { v, e -> f(v, e) })
  }

  fun andThen(ec: Executor, f: (T, Throwable) -> Unit): Future<T> {
    return Future(cf.whenCompleteAsync({ v, e -> f(v, e) }, ec))
  }

  fun onComplete(f: (Result<T>) -> Unit) {
    cf.whenComplete { v, e ->
      val result = if (e != null) Result.failure(e) else Result.success(v)
      f(result)
    }
  }

  fun onComplete(ec: Executor, f: (Result<T>) -> Unit) {
    cf.whenCompleteAsync(
      { v, e ->
        val result = if (e != null) Result.failure(e) else Result.success(v)
        f(result)
      },
      ec
    )
  }

  fun onComplete(onSuccess: (T) -> Unit, onFailure: (Throwable) -> Unit) {
    cf.whenComplete { v, e ->
      if (e != null) onFailure(e.cause ?: e)
      else onSuccess(v)
    }
  }

  fun onComplete(ec: Executor, onSuccess: (T) -> Unit, onFailure: (Throwable) -> Unit) {
    cf.whenCompleteAsync(
      { v, e ->
        if (e != null) onFailure(e.cause ?: e)
        else onSuccess(v)
      },
      ec
    )
  }

  fun onSuccess(f: (T) -> Unit): Future<T> {
    return Future(cf.whenComplete { v, e -> if (e == null) f(v) })
  }

  fun onSuccess(ec: Executor, f: (T) -> Unit): Future<T> {
    return Future(cf.whenCompleteAsync({ v, e -> if (e == null) f(v) }, ec))
  }

  fun onFailure(f: (Throwable) -> Unit): Future<T> {
    return Future(cf.whenComplete { _, e -> if (e != null) f(e.cause ?: e) })
  }

  fun onFailure(ec: Executor, f: (Throwable) -> Unit): Future<T> {
    return Future(cf.whenCompleteAsync({ _, e -> if (e != null) f(e.cause ?: e) }, ec))
  }

  fun tryRecover(f: (Throwable) -> Result<T>): Future<T> {
    val newCF = CompletableFuture<T>()
    onComplete(newCF::complete) { e ->
      val r = f(e)
      if (r.isSuccess) newCF.complete(r.getOrThrow()) else newCF.completeExceptionally(r.getCause())
    }
    return Future(newCF)
  }

  fun tryRecover(ec: Executor, f: (Throwable) -> Result<T>): Future<T> {
    val newCF = CompletableFuture<T>()
    onComplete(ec, newCF::complete) { e ->
      val r = f(e)
      if (r.isSuccess) newCF.complete(r.getOrThrow()) else newCF.completeExceptionally(r.getCause())
    }
    return Future(newCF)
  }

  fun recover(f: (Throwable) -> T): Future<T> {
    return Future(cf.exceptionally(f))
  }

  fun <E : Throwable> recover(exceptionType: Class<E>, f: (Throwable) -> T): Future<T> {
    val newCF = CompletableFuture<T>()
    onComplete(newCF::complete) { e ->
      if (!exceptionType.isAssignableFrom(e.javaClass)) {
        newCF.completeExceptionally(e)
      } else {
        @Suppress("UNCHECKED_CAST")
        newCF.completeAsync { f(e as E) }
      }
    }
    return Future(newCF)
  }

  fun <E : Throwable> recover(ec: Executor, exceptionType: Class<E>, f: (Throwable) -> T): Future<T> {
    val newCF = CompletableFuture<T>()
    onComplete(
      ec,
      newCF::complete
    ) { e ->
      if (!exceptionType.isAssignableFrom(e.javaClass)) {
        newCF.completeExceptionally(e)
      } else {
        @Suppress("UNCHECKED_CAST")
        newCF.completeAsync({ f(e as E) }, ec)
      }
    }
    return Future(newCF)
  }

  fun recoverWith(f: (Throwable) -> Future<T>): Future<T> {
    val newCF = CompletableFuture<T>()
    onComplete(newCF::complete) { e ->
      f(e).onComplete(
        {
          newCF.complete(it)
        },
        {
          newCF.completeExceptionally(it)
        }
      )
    }
    return Future(newCF)
  }

  fun recoverWith(ec: Executor, f: (Throwable) -> Future<T>): Future<T> {
    val newCF = CompletableFuture<T>()
    onComplete(
      ec,
      newCF::complete
    ) { e ->
      f(e).onComplete(
        {
          newCF.complete(it)
        },
        {
          newCF.completeExceptionally(it)
        }
      )
    }
    return Future(newCF)
  }

  fun <E : Throwable> recoverWith(exceptionType: Class<E>, f: (E) -> Future<T>): Future<T> {
    val newCF = CompletableFuture<T>()
    onComplete(newCF::complete) { e ->
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
    return Future(newCF)
  }

  fun <E : Throwable> recoverWith(ec: Executor, exceptionType: Class<E>, f: (E) -> Future<T>): Future<T> {
    val newCF = CompletableFuture<T>()
    onComplete(
      ec,
      newCF::complete
    ) { e ->
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
    return Future(newCF)
  }

  fun get(): T {
    return cf.get()
  }

  @Throws(TimeoutException::class)
  fun get(timeout: Duration): T {
    return cf.get(timeout.inWholeMilliseconds, TimeUnit.MILLISECONDS)
  }

  @Throws(TimeoutException::class)
  fun get(timeout: java.time.Duration): T {
    return cf.get(timeout.toMillis(), TimeUnit.MILLISECONDS)
  }

  @Throws(CancellationException::class, CompletionException::class, NoSuchElementException::class)
  fun getNowOrThrow(): T {
    val value = cf.unsafeCast<CompletableFuture<Any>>().getNow(DefaultValueIfAbsent)
    if (value === DefaultValueIfAbsent) {
      throw NoSuchElementException(cf.toString())
    }
    return value.unsafeCast()
  }

  @Throws(TimeoutException::class)
  fun await(timeout: Duration) {
    cf.get(timeout.inWholeMilliseconds, TimeUnit.MILLISECONDS)
  }

  @Throws(TimeoutException::class)
  fun await(timeout: java.time.Duration) {
    cf.get(timeout.toMillis(), TimeUnit.MILLISECONDS)
  }

  fun getResult(timeout: Duration): Result<T> {
    return try {
      val value = cf.get(timeout.inWholeMilliseconds, TimeUnit.MILLISECONDS)
      Result.success(value)
    } catch (e: ExecutionException) {
      Result.failure(e.cause ?: e)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  fun getResult(timeout: java.time.Duration): Result<T> {
    return getResult(timeout.toKotlinDuration())
  }

  fun isCompleted() = cf.isDone

  fun isSuccess() = cf.isDone && !cf.isCompletedExceptionally

  fun isFailed() = cf.isCompletedExceptionally

  override fun toString(): String {
    return cf.toString()
  }

  companion object {
    @JvmStatic
    private val DefaultValueIfAbsent = Any()

    operator fun <T> invoke(executor: Executor, f: () -> T): Future<T> {
      return Future(CompletableFuture.supplyAsync(f, executor))
    }

    operator fun <T> invoke(f: () -> T): Future<T> {
      return Future(CompletableFuture.supplyAsync(f))
    }

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
      val futureArray = arrayOfNulls<CompletableFuture<out T>>(futures.size)
      futures.forEachIndexed { i, f ->
        futureArray[i] = f.cf
      }
      val firstCompleted = CompletableFuture.anyOf(*futureArray)
      return Future(firstCompleted.unsafeCast())
    }

    @JvmStatic
    fun allOf(futures: Collection<Future<*>>): Future<Void> {
      val futureArray = arrayOfNulls<CompletableFuture<*>>(futures.size)
      futures.forEachIndexed { i, f ->
        futureArray[i] = f.cf
      }
      return Future(CompletableFuture.allOf(*futureArray))
    }
  }
}
