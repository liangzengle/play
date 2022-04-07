package play.util.concurrent

import play.util.getOrNull
import java.util.*
import java.util.concurrent.Flow

fun <T : Any> Flow.Publisher<T>.subscribeOne(): Future<T> {
  return subscribeOneOptional().map { it.get() }
}

fun <T : Any> Flow.Publisher<T>.subscribeOneNullable(): Future<T?> {
  return subscribeOneOptional().map { it.getOrNull() }
}

fun <T : Any> Flow.Publisher<T>.subscribeOneOptional(): Future<Optional<T>> {
  val promise = Promise.make<Optional<T>>()
  subscribe(SingleValuePromiseSubscriber(promise))
  return promise.future
}

fun <T : Any, R1 : R, R> Flow.Publisher<T>.subscribeInto(initial: R1, accumulator: (R1, T) -> R1): Future<R> {
  val promise = Promise.make<R>()
  subscribe(AccumulativePromiseSubscriber(promise, initial, accumulator))
  return promise.future
}

fun <T : Any> Flow.Publisher<T>.subscribeToList(): Future<List<T>> {
  return subscribeInto(LinkedList<T>()) { list, value ->
    list.add(value)
    list
  }
}

fun <T : Any, C : MutableCollection<T>> Flow.Publisher<T>.subscribeToCollection(initial: C): Future<C> {
  return subscribeInto(initial) { c, value ->
    c.add(value)
    c
  }
}
