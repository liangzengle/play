@file:Suppress("NOTHING_TO_INLINE")

package play.util.control

inline fun <T> Result<T>.exists(f: (T) -> Boolean): Boolean = if (isSuccess) f(getOrThrow()) else false

inline fun <T> Result<T>.getCause(): Throwable = checkNotNull(exceptionOrNull())

inline fun <T> failure(e: Throwable): Result<T> = Result.failure(e)

inline fun <T> success(t: T): Result<T> = Result.success(t)
