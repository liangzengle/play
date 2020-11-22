package play.util.collection

import org.jctools.maps.NonBlockingHashMapLong
import play.util.unsafeCast
import java.util.*

fun <T> NonBlockingHashMapLong<T>.keysIterator(): PrimitiveIterator.OfLong =
  keys().unsafeCast<NonBlockingHashMapLong<T>.IteratorLong>().toJava()

fun <T> NonBlockingHashMapLong<T>.IteratorLong.toJava(): PrimitiveIterator.OfLong {
  return object : PrimitiveIterator.OfLong {
    override fun remove() {
      return this@toJava.remove()
    }

    override fun hasNext(): Boolean {
      return this@toJava.hasNext()
    }

    override fun nextLong(): Long {
      return this@toJava.nextLong()
    }
  }
}
