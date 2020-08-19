package play.db.mongo

import io.vavr.concurrent.Promise
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription

internal class ForOneSubscriber<T>(private val promise: Promise<T>) : Subscriber<T> {
  override fun onSubscribe(subscription: Subscription) {
    subscription.request(1)
  }

  override fun onNext(item: T) {
    promise.success(item)
  }

  override fun onError(throwable: Throwable) {
    promise.failure(throwable)
  }

  override fun onComplete() {
    promise.trySuccess(null)
  }
}

internal class FoldSubscriber<T, R1 : R2, R2>(
  private val promise: Promise<R2>,
  initial: R1,
  val folder: (R1, T) -> R1
) : Subscriber<T> {
  private var result = initial

  override fun onSubscribe(subscription: Subscription) {
    subscription.request(Long.MAX_VALUE)
  }

  override fun onNext(item: T) {
    result = folder(result, item)
  }

  override fun onError(throwable: Throwable) {
    promise.failure(throwable)
  }

  override fun onComplete() {
    promise.success(result)
  }
}
