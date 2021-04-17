@file:Suppress("NOTHING_TO_INLINE")
@file:JvmName("ObjectUtil")

package play.util

@Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST")
inline fun <T> Any.unsafeCast(): T = this as T

@Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST")
inline fun <reified T> Any?.unsafeCastOrNull(): T? =
  if (this !== null && T::class.java.isAssignableFrom(this.javaClass)) this as T else null

/**
 * if (test(this)) this else null
 *
 * @receiver T
 * @param test 条件判断
 * @return T?
 */
inline fun <T : Any> T.filterOrNull(test: (T) -> Boolean): T? = if (test(this)) this else null

/**
 * if (!test(this)) this else null
 *
 * @receiver T
 * @param test 条件判断
 * @return T?
 */
inline fun <T : Any> T.filterNotOrNull(test: (T) -> Boolean): T? = if (!test(this)) this else null

inline fun <T> unsafeLazy(noinline initializer: () -> T): Lazy<T> = lazy(LazyThreadSafetyMode.NONE, initializer)

inline fun <T : Any> casLazy(noinline initializer: () -> T): Lazy<T> =
  lazy(LazyThreadSafetyMode.PUBLICATION, initializer)
