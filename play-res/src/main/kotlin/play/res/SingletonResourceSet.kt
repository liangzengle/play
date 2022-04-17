package play.res

import java.util.*

interface SingletonResourceSet<T> {
  fun get(): T
}

internal class SingletonResourceSetImpl<T : AbstractResource>(private val elem: T) : ResourceSet<T>,
  SingletonResourceSet<T> {

  override fun get(): T = elem

  override fun indexOf(elem: T): Int = if (elem.id == this.elem.id) 0 else -1
  override fun getByIndexOrNull(idx: Int): T = if (idx == 0) elem else throw ArrayIndexOutOfBoundsException(idx)
  override fun contains(id: Int): Boolean = elem.id == id
  override fun get(id: Int): Optional<T> = if (elem.id == id) Optional.of(elem) else Optional.empty()
  override fun getOrThrow(id: Int): T = if (elem.id == id) elem else throw NoSuchElementException(id.toString())
  override fun getOrNull(id: Int): T? = if (elem.id == id) elem else null
  override fun list(): List<T> = listOf(elem)
}
