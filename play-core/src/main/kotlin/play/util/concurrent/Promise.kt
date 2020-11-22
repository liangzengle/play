package play.util.concurrent

import java.util.concurrent.CompletableFuture

typealias PlayPromise<T> = Promise<T>

/**
 * A wrapper of CompletableFuture
 * @author LiangZengle
 */
class Promise<T>(private val cf: CompletableFuture<T>) {

  val future: Future<T> = Future(cf)

  fun complete(result: Result<T>) {
    result.fold(cf::complete, cf::completeExceptionally)
  }

  fun completeWith(f: Future<out T>) {
    f.onComplete(::complete)
  }

  fun success(value: T) = cf.complete(value)

  fun trySuccess(value: T): Boolean = cf.complete(value)

  fun failure(e: Throwable) {
    cf.completeExceptionally(e)
  }

  fun tryFailure(e: Throwable): Boolean = cf.completeExceptionally(e)

  fun isCompleted() = cf.isDone

  companion object {
    @JvmStatic
    fun <T> make(): Promise<T> {
      return Promise(CompletableFuture())
    }
  }
}
