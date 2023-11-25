package play.util.control

import com.google.common.collect.Lists
import play.util.unsafeCast
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

  fun getCode(): Int = (value as? Err)?.code ?: 0

  fun asErr(): Err = value as Err

  fun <U> asErrResult(): Result2<U> = Result2(asErr())

  override fun toString(): String {
    return if (isErr()) asErr().toString() else "Ok($value)"
  }

  data class Err(@JvmField val code: Int, @JvmField val args: List<String>) {

    override fun toString(): String {
      return if (args.isEmpty()) "Err($code)" else "Err($code, $args)"
    }
  }
}

private val ok = Result2<Nothing>(Unit)
fun <T> ok(): Result2<T> = ok

fun <T : Any> ok(value: T): Result2<T> = Result2(value)

inline fun <T : Any> ok(supplier: () -> T): Result2<T> = Result2(supplier())

fun err(code: Int): Result2<Nothing> = Result2(Result2.Err(code, emptyList()))

fun err(code: Int, arg0: String, vararg restArgs: String): Result2<Nothing> {
  return if (restArgs.isEmpty()) Result2(Result2.Err(code, listOf(arg0)))
  else Result2(Result2.Err(code, Lists.asList(arg0, restArgs)))
}

fun err(code: Int, arg0: Any, vararg restArgs: Any?): Result2<Nothing> {
  return if (restArgs.isEmpty()) Result2(Result2.Err(code, listOf(arg0.toString())))
  else {
    val args = Lists.asList(arg0.toString(), Array(restArgs.size) { i -> restArgs[i]?.toString().orEmpty() })
    Result2(Result2.Err(code, args))
  }
}

fun Result2<Nothing>.withArgs(arg0: String, vararg restArgs: String): Result2<Nothing> {
  return if (this.isOk()) this else err(getCode(), arg0, restArgs)
}

fun Result2<Nothing>.withArgs(arg0: Any, vararg restArgs: Any?): Result2<Nothing> {
  return if (this.isOk()) this else err(getCode(), arg0, restArgs)
}

inline fun <R : Any, T : R> Result2<T>.recover(f: () -> R): Result2<R> {
  return if (isErr()) ok(f()) else this
}

inline fun <R : Any, T : R> Result2<T>.recover(f: (Int) -> R): Result2<R> {
  return if (isErr()) ok(f(getCode())) else this
}

inline fun <R : Any, T : R> Result2<T>.recover(filter: (Int) -> Boolean, f: (Int) -> R): Result2<R> {
  return if (isErr() && filter(getCode())) ok(f(getCode())) else this
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

inline fun <T> Result2<T>.onOk(f: (T) -> Unit): Result2<T> {
  forEach(f)
  return this
}
