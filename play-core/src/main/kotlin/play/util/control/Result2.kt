package play.util.control

import java.io.Serializable
import java.util.*
import play.util.unsafeCast

@Suppress("UNCHECKED_CAST")
inline class Result2<out T>(private val value: Any?) {

  fun isOk() = value !is Err

  fun isErr() = value is Err

  fun hasValue() = isOk() && value != null

  fun get(): T = value as T

  fun <R, T : R> getOrDefault(value: R): R {
    return if (isErr()) value else this.value as R
  }

  fun getOrNull(): T? = if (hasValue()) value as T? else null

  fun <T> toOption(): Optional<T> = if (isErr()) Optional.empty() else Optional.ofNullable(value).unsafeCast()

  fun getErrorCode(): Int = (value as? Err)?.code ?: 0

  fun asErr(): Err = value as Err

  fun <T> asErrResult(): Result2<T> = Result2(asErr())

  override fun toString(): String {
    return if (isErr()) "Err(${getErrorCode()})" else "Ok($value)"
  }

  class Err(@JvmField val code: Int) : Serializable {

    override fun equals(other: Any?): Boolean {
      return other is Err && code == other.code
    }

    override fun hashCode(): Int {
      return code.hashCode()
    }

    override fun toString(): String {
      return "Err($code)"
    }
  }
}

fun <T> ok(): Result2<T> = Result2<Nothing>(Unit)

fun <T> ok(value: T): Result2<T> = Result2(value)

inline fun <T> ok(supplier: () -> T): Result2<T> = Result2(supplier())

fun err(code: Int): Result2<Nothing> = Result2(Result2.Err(code))

inline fun <R, T : R> Result2<T>.recover(f: () -> R): Result2<R> {
  return if (isErr()) ok(f()) else this
}

inline fun <R, T : R> Result2<T>.recover(f: (Int) -> R): Result2<R> {
  return if (isErr()) ok(f(getErrorCode())) else this
}

@Suppress("UNCHECKED_CAST")
inline fun <R, T> Result2<T>.map(f: (T) -> R): Result2<R> {
  return if (isOk()) ok(f(get())) else this as Result2<R>
}

@Suppress("UNCHECKED_CAST")
inline fun <R, T> Result2<T>.flatMap(f: (T) -> Result2<R>): Result2<R> {
  return if (isOk()) f(get()) else this as Result2<R>
}

inline fun <T> Result2<T>.forEach(f: (T) -> Unit) {
  if (isOk()) f(get())
}
