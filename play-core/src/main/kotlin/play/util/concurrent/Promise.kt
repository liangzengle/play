package play.util.concurrent

import java.util.concurrent.CompletableFuture

typealias PlayPromise<T> = Promise<T>

/**
 * A wrapper of CompletableFuture
 * @author LiangZengle
 */
@JvmInline
value class Promise<T>(private val cf: CompletableFuture<T>) {

  val future: Future<T> get() = Future(cf)

  fun complete(result: Result<T>) {
    result.fold(cf::complete, cf::completeExceptionally)
  }

  fun completeWith(f: Future<out T>) {
    f.onComplete(::complete)
  }

  /**
   * Exception is properly handled.
   * @param f
   */
  inline fun catchingComplete(f: () -> T) {
    complete(runCatching(f))
  }

  fun success(value: T) = cf.complete(value)

  fun trySuccess(value: T): Boolean = cf.complete(value)

  fun failure(e: Throwable) {
    cf.completeExceptionally(e)
  }

  fun tryFailure(e: Throwable): Boolean = cf.completeExceptionally(e)

  fun isCompleted() = cf.isDone

  override fun toString(): String {
    return cf.toString()
  }

  companion object {
    @JvmStatic
    fun <T> make(): Promise<T> {
      return Promise(CompletableFuture())
    }

    @JvmStatic
    fun <T> successful(value: T): Promise<T> {
      val promise = make<T>()
      promise.success(value)
      return promise
    }

    @JvmStatic
    fun <T> failure(value: Throwable): Promise<T> {
      val promise = make<T>()
      promise.failure(value)
      return promise
    }

    @JvmStatic
    fun <T> from(result: Result<T>): Promise<T> {
      val promise = make<T>()
      promise.complete(result)
      return promise
    }
  }
}
