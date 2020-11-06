package play.util.concurrent

import java.util.concurrent.CompletableFuture

typealias PlayPromise<T> = Promise<T>

/**
 * A wrapper of CompletableFuture
 * @author LiangZengle
 */
inline class Promise<T>(private val cf: CompletableFuture<T>) {

  val future: Future<T> get() = Future(cf)

  fun complete(value: T) {
    cf.complete(value)
  }

  fun complete(f: () -> T) {
    cf.completeAsync(f)
  }

  fun completeWith(f: Future<out T>) {
    f.onComplete(::completeWith)
  }

  fun completeWith(result: Result<T>) {
    result.fold(cf::complete, cf::completeExceptionally)
  }

  fun success(value: T) = complete(value)

  fun trySuccess(value: T): Boolean = cf.complete(value)

  fun failure(e: Throwable) {
    cf.completeExceptionally(e)
  }

  fun tryFailure(e: Throwable): Boolean = cf.completeExceptionally(e)

  fun isCompleted() = cf.isDone

  companion object {
    fun <T> make(): Promise<T> {
      return Promise(CompletableFuture())
    }
  }
}
