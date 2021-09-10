package play.res

import java.util.*

interface SingletonResourceSet<T> {
  fun get(): T
}

internal class SingletonResourceSetImpl<T : AbstractResource>(private val elem: T) : ResourceSet<T>,
  SingletonResourceSet<T> {

  override fun get(): T = elem

  override fun indexOf(elem: T): Int = throw UnsupportedOperationException()
  override fun getByIndexOrNull(idx: Int): T = throw UnsupportedOperationException()
  override fun contains(id: Int): Boolean = throw UnsupportedOperationException()
  override fun get(id: Int): Optional<T> = throw UnsupportedOperationException()
  override fun getOrThrow(id: Int): T = throw UnsupportedOperationException()
  override fun getOrNull(id: Int): T = throw UnsupportedOperationException()
  override fun list(): List<T> = listOf(elem)
}
