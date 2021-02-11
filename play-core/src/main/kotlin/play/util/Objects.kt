@file:Suppress("NOTHING_TO_INLINE")

package play.util

@Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST")
inline fun <T> Any.unsafeCast(): T = this as T

@Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST")
inline fun <reified T> Any?.unsafeCastOrNull(): T? =
  if (this !== null && T::class.java.isAssignableFrom(this.javaClass)) this as T else null

/**
 * if (f(this)) this else null
 *
 * @receiver T
 * @param f 条件判断
 * @return T?
 */
inline fun <T : Any> T.filterOrNull(f: (T) -> Boolean): T? = if (f(this)) this else null

/**
 * if (!f(this)) this else null
 *
 * @receiver T
 * @param f 条件判断
 * @return T?
 */
inline fun <T : Any> T.filterNotOrNull(f: (T) -> Boolean): T? = if (!f(this)) this else null

inline fun <T> unsafeLazy(noinline initializer: () -> T): Lazy<T> = lazy(LazyThreadSafetyMode.NONE, initializer)

inline fun <T> casLazy(noinline initializer: () -> T): Lazy<T> = lazy(LazyThreadSafetyMode.PUBLICATION, initializer)
