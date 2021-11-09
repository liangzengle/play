package play.util.control

import play.util.unsafeCast
import java.io.Serializable
import java.util.*

@Suppress("UNCHECKED_CAST")
@JvmInline
value class Result2<out T>(private val value: Any) {

  fun isOk() = value !is Err

  fun isErr() = value is Err

  fun get(): T = value as T

  fun getOrDefault(value: @UnsafeVariance T): T {
    return if (isErr()) value else this.value as T
  }

  fun toOption(): Optional<out T> = if (isErr()) Optional.empty() else Optional.ofNullable(value.unsafeCast())

  fun getErrorCode(): Int = (value as? Err)?.code ?: 0

  fun asErr(): Err = value as Err

  fun <U> asErrResult(): Result2<U> = Result2(asErr())

  operator fun invoke(msg: String): Result2<T> = if (isErr()) err(getErrorCode(), msg) else this

  override fun toString(): String {
    return if (isErr()) asErr().toString() else "Ok($value)"
  }

  class Err(@JvmField val code: Int, @JvmField val msg: String?) : Serializable {
    constructor(code: Int) : this(code, null)

    override fun toString(): String {
      return if (msg == null) "Err($code)" else "Err($code, $msg)"
    }

    override fun equals(other: Any?): Boolean {
      return this === other || (other is Err && other.code == this.code)
    }

    override fun hashCode(): Int {
      return code
    }
  }
}

private val ok = Result2<Nothing>(Unit)
fun <T> ok(): Result2<T> = ok

fun <T : Any> ok(value: T): Result2<T> = Result2(value)

inline fun <T : Any> ok(supplier: () -> T): Result2<T> = Result2(supplier())

fun err(code: Int): Result2<Nothing> = Result2(Result2.Err(code))

fun err(code: Int, msg: String): Result2<Nothing> = Result2(Result2.Err(code, msg))

inline fun <R : Any, T : R> Result2<T>.recover(f: () -> R): Result2<R> {
  return if (isErr()) ok(f()) else this
}

inline fun <R : Any, T : R> Result2<T>.recover(f: (Int) -> R): Result2<R> {
  return if (isErr()) ok(f(getErrorCode())) else this
}

inline fun <R : Any, T : R> Result2<T>.recover(filter: (Int) -> Boolean, f: (Int) -> R): Result2<R> {
  return if (isErr() && filter(getErrorCode())) ok(f(getErrorCode())) else this
}

@Suppress("UNCHECKED_CAST")
inline fun <R : Any, T> Result2<T>.map(f: (T) -> R): Result2<R> {
  return if (isOk()) ok(f(get())) else this as Result2<R>
}

@Suppress("UNCHECKED_CAST")
inline fun <R, T> Result2<T>.flatMap(f: (T) -> Result2<R>): Result2<R> {
  return if (isOk()) f(get()) else this as Result2<R>
}

inline fun <T> Result2<T>.forEach(f: (T) -> Unit) {
  if (isOk()) f(get())
}

inline fun <T> Result2<T>.peek(f: (T) -> Unit): Result2<T> {
  forEach(f)
  return this
}
