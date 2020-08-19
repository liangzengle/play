@file:Suppress("NOTHING_TO_INLINE")

package play.util.collection

import java.util.*

inline fun OptionalLong.forEach(f: (Long) -> Unit) {
  if (isPresent) f(asLong)
}

inline fun OptionalLong.map(f: (Long) -> Long): OptionalLong {
  return if (isPresent) OptionalLong.of(f(asLong)) else this
}

inline fun OptionalLong.flatMap(f: (Long) -> OptionalLong): OptionalLong {
  return if (isPresent) f(asLong) else this
}

inline fun <T : Any> OptionalLong.mapToObject(f: (Long) -> T): Optional<T> {
  return if (isPresent) Optional.of(f(asLong)) else Optional.empty()
}

inline fun OptionalInt.forEach(f: (Int) -> Unit) {
  if (isPresent) f(asInt)
}

inline fun OptionalInt.map(f: (Int) -> Int): OptionalInt {
  return if (isPresent) OptionalInt.of(f(asInt)) else this
}

inline fun OptionalInt.flatMap(f: (Int) -> OptionalInt): OptionalInt {
  return if (isPresent) f(asInt) else this
}

inline fun <T : Any> OptionalInt.flatMapToObject(f: (Int) -> T): Optional<T> {
  return if (isPresent) Optional.of(f(asInt)) else Optional.empty()
}

inline fun <T> Optional<T>.getOrNull(): T? = if (isPresent) get() else null



