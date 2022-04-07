package play.util.concurrent

import java.util.*
import java.util.concurrent.Flow

internal class SingleValuePromiseSubscriber<T : Any>(private val promise: Promise<Optional<T>>) : Flow.Subscriber<T> {
  override fun onSubscribe(subscription: Flow.Subscription) {
    promise.future.onComplete {
      subscription.cancel()
    }
    subscription.request(1)
  }

  override fun onNext(item: T?) {
    promise.success(Optional.ofNullable(item))
  }

  override fun onError(throwable: Throwable) {
    promise.failure(throwable)
  }

  override fun onComplete() {
    promise.success(Optional.empty())
  }
}

internal class AccumulativePromiseSubscriber<T, R1 : R, R>(
  private val promise: Promise<R>,
  initial: R1,
  val accumulator: (R1, T) -> R1
) : Flow.Subscriber<T> {
  private var result = initial

  override fun onSubscribe(subscription: Flow.Subscription) {
    subscription.request(Long.MAX_VALUE)
    promise.future.onComplete { subscription.cancel() }
  }

  override fun onNext(item: T) {
    try {
      result = accumulator(result, item)
    } catch (e: Throwable) {
      promise.failure(e)
    }
  }

  override fun onError(throwable: Throwable) {
    promise.failure(throwable)
  }

  override fun onComplete() {
    promise.success(result)
  }
}
