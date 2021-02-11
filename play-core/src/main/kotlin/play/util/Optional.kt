@file:Suppress("NOTHING_TO_INLINE")

package play.util

import java.util.*

inline fun <T> T?.toOptional() = Optional.ofNullable(this)

inline fun Int?.toOptional(): OptionalInt = if (this == null) OptionalInt.empty() else OptionalInt.of(this)

inline fun Long?.toOptional(): OptionalLong = if (this == null) OptionalLong.empty() else OptionalLong.of(this)

inline fun <T> empty() = Optional.empty<T>()
inline fun emptyInt() = OptionalInt.empty()
inline fun emptyLong() = OptionalLong.empty()

inline fun OptionalLong.forEach(f: (Long) -> Unit) {
  if (isPresent) f(asLong)
}

inline fun OptionalLong.map(f: (Long) -> Long): OptionalLong {
  return if (isPresent) OptionalLong.of(f(asLong)) else this
}

inline fun <T : Any> OptionalLong.mapToObj(f: (Long) -> T): Optional<T> {
  return if (isPresent) Optional.of(f(asLong)) else Optional.empty()
}

inline fun OptionalLong.flatMap(f: (Long) -> OptionalLong): OptionalLong {
  return if (isPresent) f(asLong) else this
}

inline fun OptionalInt.forEach(f: (Int) -> Unit) {
  if (isPresent) f(asInt)
}

inline fun OptionalInt.map(f: (Int) -> Int): OptionalInt {
  return if (isPresent) OptionalInt.of(f(asInt)) else this
}

inline fun <T : Any> OptionalInt.mapToObj(f: (Int) -> T): Optional<T> {
  return if (isPresent) Optional.of(f(asInt)) else Optional.empty()
}

inline fun OptionalInt.flatMap(f: (Int) -> OptionalInt): OptionalInt {
  return if (isPresent) f(asInt) else this
}

inline fun OptionalDouble.forEach(f: (Double) -> Unit) {
  if (isPresent) f(asDouble)
}

inline fun OptionalDouble.map(f: (Double) -> Double): OptionalDouble {
  return if (isPresent) OptionalDouble.of(f(asDouble)) else this
}

inline fun <T : Any> OptionalDouble.mapToObj(f: (Double) -> T): Optional<T> {
  return if (isPresent) Optional.of(f(asDouble)) else Optional.empty()
}

inline fun OptionalDouble.flatMap(f: (Double) -> OptionalDouble): OptionalDouble {
  return if (isPresent) f(asDouble) else this
}

inline fun <T> Optional<T>.getOrNull(): T? = if (isPresent) get() else null

inline fun <T> Optional<T>.contains(expect: T): Boolean = if (isPresent) expect == get() else false

inline fun <T> Optional<T>.exists(test: (T) -> Boolean): Boolean = if (isPresent) test(get()) else false

inline fun <T> Optional<T>.forEach(f: (T) -> Unit) {
  if (isPresent) f(get())
}

inline fun <T, R> Optional<T>.fold(initial: R, f: (T) -> R?): R {
  return if (isEmpty) initial else f(get()) ?: initial
}
