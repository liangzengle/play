package play.inject

import javax.inject.Provider
import play.util.unsafeCast

/**
 *
 * @author LiangZengle
 */
class NullProvider<T> private constructor() : Provider<T> {
  override fun get(): T? = null

  companion object {
    private val NULL = NullProvider<Any>()

    @JvmStatic
    fun <T> of(): Provider<T> = NULL.unsafeCast()
  }
}
